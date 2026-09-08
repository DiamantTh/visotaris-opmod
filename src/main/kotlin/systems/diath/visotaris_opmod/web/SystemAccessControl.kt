package systems.diath.visotaris_opmod.web

import systems.diath.visotaris_opmod.config.ConfigManager
import systems.diath.visotaris_opmod.security.SystemPasswordService
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/** Kurzlebige, nur im Speicher liegende Sitzungen für /system und /api/system. */
class SystemAccessControl(private val config: ConfigManager) {
    companion object {
        const val COOKIE = "visotaris_system_session"
        const val SESSION_SECONDS = 30 * 60L
    }

    private val random = SecureRandom()
    private val sessions = ConcurrentHashMap<String, Long>()
    private var failedAttempts = 0
    private var blockedUntil = 0L

    @Synchronized fun setup(password: CharArray): String {
        if (configured()) throw IllegalStateException("Das Systempasswort ist bereits eingerichtet.")
        config.config.systemPasswordHash = SystemPasswordService.hash(password)
        config.save()
        return createSession()
    }

    @Synchronized fun login(password: CharArray): String? {
        if (!configured() || System.currentTimeMillis() < blockedUntil) return null
        if (!SystemPasswordService.verify(password, config.config.systemPasswordHash)) {
            failedAttempts++
            if (failedAttempts >= 5) { blockedUntil = System.currentTimeMillis() + 5 * 60_000L; failedAttempts = 0 }
            return null
        }
        failedAttempts = 0
        return createSession()
    }

    fun configured() = SystemPasswordService.isConfigured(config.config.systemPasswordHash)
    fun authenticated(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val expires = sessions[token] ?: return false
        if (expires < System.currentTimeMillis()) { sessions.remove(token); return false }
        return true
    }
    fun logout(token: String?) { if (token != null) sessions.remove(token) }

    private fun createSession(): String {
        val bytes = ByteArray(32); random.nextBytes(bytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        sessions[token] = System.currentTimeMillis() + SESSION_SECONDS * 1_000L
        return token
    }
}
