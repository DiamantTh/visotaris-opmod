package systems.diath.visotaris_opmod.model;

/**
 * Hält einen ausstehenden, bestätigungspflichtigen Befehl.
 * Wird vom {@code PendingConfirmationService} für /rename und /sign verwaltet.
 *
 * Die Validierung prüft:
 *   - Slot-Wechsel (slot != savedSlot → ungültig)
 *   - Item-Wechsel (itemFingerprint != savedFingerprint → ungültig)
 *   - Timeout     (System.currentTimeMillis() > timestamp + timeoutMs → ungültig)
 */
public final class PendingAction {

    public enum Type { RENAME, SIGN }

    private final Type   type;
    private final String command;
    private final String text;
    private final String itemFingerprint;
    private final int    slot;
    private final long   timestamp;
    private final long   timeoutMs;

    public PendingAction(Type type, String command, String text,
                         String itemFingerprint, int slot, long timeoutMs) {
        this.type            = type;
        this.command         = command;
        this.text            = text;
        this.itemFingerprint = itemFingerprint;
        this.slot            = slot;
        this.timestamp       = System.currentTimeMillis();
        this.timeoutMs       = timeoutMs;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > timestamp + timeoutMs;
    }

    public Type   getType()            { return type; }
    public String getCommand()         { return command; }
    public String getText()            { return text; }
    public String getItemFingerprint() { return itemFingerprint; }
    public int    getSlot()            { return slot; }
}
