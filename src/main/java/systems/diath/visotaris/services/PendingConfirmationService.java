package systems.diath.visotaris.services;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import systems.diath.visotaris.VisotarisLogger;
import systems.diath.visotaris.config.ConfigManager;
import systems.diath.visotaris.model.PendingAction;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Verwaltet ausstehende Bestätigungsaktionen für /rename und /sign.
 *
 * Ablauf:
 *   1. Command-Intercept speichert PendingAction
 *   2. Spieler bestätigt mit /.confirmRename / /.confirmSign
 *   3. Validierung prüft Slot, Item-Fingerprint und Timeout
 *   4. Bei Erfolg: Original-Command an Server schicken
 *
 * Schutzlogik ist bewusst hier, nicht im Mixin selbst.
 */
public final class PendingConfirmationService {

    private static final long DEFAULT_TIMEOUT_MS = 30_000L;

    private final ConfigManager config;
    private final AtomicReference<PendingAction> pending = new AtomicReference<>();

    public PendingConfirmationService(ConfigManager config) {
        this.config = config;
    }

    /**
     * Speichert eine neue ausstehende Aktion.
     * @return {@code false} wenn bereits eine andere Aktion aussteht.
     */
    public boolean queue(PendingAction.Type type, String command, String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        ItemStack held = mc.player.getMainHandStack();
        String fingerprint = makeFingerprint(held);
        int slot = mc.player.getInventory().selectedSlot;

        PendingAction action = new PendingAction(
            type, command, text, fingerprint, slot, DEFAULT_TIMEOUT_MS
        );
        pending.set(action);
        VisotarisLogger.debug("PendingAction gespeichert: {} slot={}", type, slot);
        return true;
    }

    /**
     * Bestätigt und führt die gespeicherte Aktion aus, falls gültig.
     */
    public boolean confirm(PendingAction.Type type) {
        PendingAction action = pending.getAndSet(null);
        if (action == null) return false;
        if (action.getType() != type) { pending.set(action); return false; }
        if (!validate(action)) return false;

        // Befehl an den Server senden
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand(action.getCommand());
        }
        return true;
    }

    public void cancel(PendingAction.Type type) {
        PendingAction action = pending.get();
        if (action != null && action.getType() == type) {
            pending.set(null);
            VisotarisLogger.debug("PendingAction abgebrochen: {}", type);
        }
    }

    /**
     * Datenklasse für das Ergebnis von {@link #tryIntercept(String)}.
     */
    public record Intercepted(PendingAction.Type type, String text,
                              String confirmCmd, String cancelCmd) {}

    /**
     * Versucht, /rename oder /sign abzufangen.
     *
     * @param rawCommand  Befehl OHNE führenden '/' (wie Fabric ihn liefert).
     * @return {@link Intercepted} wenn abgefangen und in Queue gestellt,
     *         {@code null} wenn kein geschützter Befehl.
     */
    public Intercepted tryIntercept(String rawCommand) {
        var cfg = config.getConfig();

        PendingAction.Type type;
        String prefix;
        String confirmCmd;
        String cancelCmd;

        if (cfg.enableRenameProtection && rawCommand.startsWith("rename ")) {
            type       = PendingAction.Type.RENAME;
            prefix     = "rename ";
            confirmCmd = "/.confirmRename";
            cancelCmd  = "/.cancelRename";
        } else if (cfg.enableSignProtection && rawCommand.startsWith("sign ")) {
            type       = PendingAction.Type.SIGN;
            prefix     = "sign ";
            confirmCmd = "/.confirmSign";
            cancelCmd  = "/.cancelSign";
        } else {
            return null;
        }

        String text   = rawCommand.substring(prefix.length());
        boolean queued = queue(type, rawCommand, text);
        if (!queued) {
            VisotarisLogger.debug("Bereits eine PendingAction ausstehend – neuer Intercept ignoriert.");
            return null; // alte Aktion schützen
        }
        return new Intercepted(type, text, confirmCmd, cancelCmd);
    }

    public boolean hasPending(PendingAction.Type type) {
        PendingAction action = pending.get();
        return action != null && action.getType() == type && !action.isExpired();
    }

    private boolean validate(PendingAction action) {
        if (action.isExpired()) {
            VisotarisLogger.debug("PendingAction abgelaufen.");
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        int currentSlot = mc.player.getInventory().selectedSlot;
        if (currentSlot != action.getSlot()) {
            VisotarisLogger.debug("PendingAction ungültig: Slot gewechselt ({} → {}).",
                action.getSlot(), currentSlot);
            return false;
        }

        String currentFingerprint = makeFingerprint(mc.player.getMainHandStack());
        if (!currentFingerprint.equals(action.getItemFingerprint())) {
            VisotarisLogger.debug("PendingAction ungültig: Item gewechselt.");
            return false;
        }
        return true;
    }

    /**
     * Einfacher Fingerabdruck aus Item-ID und Custom-Name.
     * TODO: Bei Bedarf NBT-/Komponenten-Hash einbeziehen.
     */
    private static String makeFingerprint(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        // hasCustomName() wurde in 1.21.x durch Data-Components ersetzt
        String name = stack.contains(DataComponentTypes.CUSTOM_NAME)
            ? stack.getName().getString() : "";
        return stack.getItem() + ":" + name;
    }
}
