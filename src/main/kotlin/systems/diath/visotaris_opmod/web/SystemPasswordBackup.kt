package systems.diath.visotaris_opmod.web

import systems.diath.visotaris_opmod.VisotarisLogger
import systems.diath.visotaris_opmod.config.ConfigManager
import systems.diath.visotaris_opmod.security.SystemPasswordService
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.AclEntry
import java.nio.file.attribute.AclEntryPermission
import java.nio.file.attribute.AclEntryType
import java.nio.file.attribute.AclFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

/**
 * Redundante, lokale Sperre für den System-Hash.
 * Sie schützt gegen versehentliches Löschen einer einzelnen TOML-Zeile, nicht
 * gegen einen Angreifer mit Schreibrecht auf den gesamten Configordner.
 */
class SystemPasswordBackup(private val config: ConfigManager) {
    private val primary: Path? = config.configPath?.resolveSibling("visotaris.system-auth")
    private val backup: Path? = config.configPath?.resolveSibling("visotaris.system-auth.bak")

    @Synchronized fun activeHash(): String {
        val stored = read(primary) ?: read(backup)
        val configured = config.config.systemPasswordHash
        return when {
            SystemPasswordService.isConfigured(stored) -> {
                if (stored != configured) { config.config.systemPasswordHash = stored; config.save() }
                stored ?: ""
            }
            SystemPasswordService.isConfigured(configured) -> {
                persist(configured); configured
            }
            else -> ""
        }
    }

    @Synchronized fun persist(hash: String) {
        require(SystemPasswordService.isConfigured(hash))
        write(primary, hash)
        write(backup, hash)
    }

    private fun read(path: Path?): String? = try {
        if (path == null || !Files.isRegularFile(path)) null
        else Files.readString(path, StandardCharsets.UTF_8).lineSequence().drop(1).firstOrNull()
            ?.takeIf(SystemPasswordService::isConfigured)
    } catch (e: Exception) {
        VisotarisLogger.warn("System-Hash-Sicherung konnte nicht gelesen werden: {}", e.message); null
    }

    private fun write(path: Path?, hash: String) {
        if (path == null) return
        try {
            Files.createDirectories(path.parent)
            val temporary = path.resolveSibling(path.fileName.toString() + ".tmp")
            Files.writeString(temporary, "VISOTARIS_SYSTEM_AUTH_V1\n$hash\n", StandardCharsets.UTF_8)
            restrict(temporary)
            try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
            catch (_: Exception) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING) }
            restrict(path)
        } catch (e: Exception) { VisotarisLogger.warn("System-Hash-Sicherung konnte nicht geschrieben werden: {}", e.message) }
    }

    /** POSIX: 0600. Windows: explizite Vollrechte nur für den Datei-Eigentümer. */
    private fun restrict(path: Path) {
        try {
            val posix = Files.getFileAttributeView(path, java.nio.file.attribute.PosixFileAttributeView::class.java)
            if (posix != null) { Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------")); return }
            val acl = Files.getFileAttributeView(path, AclFileAttributeView::class.java) ?: return
            val owner = Files.getOwner(path)
            val ownerEntry = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner)
                .setPermissions(AclEntryPermission.values().toSet()).build()
            acl.setAcl(listOf(ownerEntry))
        } catch (e: Exception) { VisotarisLogger.warn("Dateirechte für System-Hash konnten nicht eingeschränkt werden: {}", e.message) }
    }
}
