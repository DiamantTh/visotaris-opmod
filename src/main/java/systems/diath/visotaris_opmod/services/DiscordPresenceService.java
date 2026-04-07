package systems.diath.visotaris_opmod.services;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import systems.diath.visotaris_opmod.VisotarisLogger;
import systems.diath.visotaris_opmod.config.ConfigManager;

/**
 * Discord Rich Presence – Lifecycle-Stub.
 *
 * Standardmäßig deaktiviert (enableDiscordRpc = false), weil viele Spieler
 * bereits eine dedizierte RPC-Mod haben. Dieser Service belegt in dem Fall
 * keinerlei Discord-IPC-Ressourcen.
 *
 * Aktivierungsschritte wenn benötigt:
 *   1. discord-ipc oder discord-game-sdk Bibliothek als Dependency einbinden
 *   2. connect() / disconnect() hier implementieren
 *   3. updateState() aus onJoin / onTick aufrufen
 */
public final class DiscordPresenceService {

    private final ConfigManager config;
    private volatile boolean active = false;

    public DiscordPresenceService(ConfigManager config) {
        this.config = config;
    }

    /**
     * Lifecycle-Events registrieren. Wird einmalig beim Mod-Start aufgerufen.
     * Der eigentliche Connect passiert erst beim Server-Join.
     */
    public void registerEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client)   -> onDisconnect());
    }

    // ── Private Lifecycle ─────────────────────────────────────────────────────

    private void onJoin() {
        if (!config.getConfig().enableDiscordRpc) return;
        VisotarisLogger.debug("DiscordPresence: JOIN – RPC aktiv");
        active = true;
        // TODO: Discord IPC connect() + initiale Presence setzen
    }

    private void onDisconnect() {
        if (!active) return;
        active = false;
        VisotarisLogger.debug("DiscordPresence: DISCONNECT – RPC deaktiviert");
        // TODO: Discord IPC disconnect()
    }
}
