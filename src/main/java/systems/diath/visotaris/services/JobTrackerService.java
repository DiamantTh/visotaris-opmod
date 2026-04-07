package systems.diath.visotaris.services;

import systems.diath.visotaris.VisotarisLogger;
import systems.diath.visotaris.config.ConfigManager;
import systems.diath.visotaris.model.JobSnapshot;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leitet den Jobzustand aus eingehenden Chat-Nachrichten ab.
 *
 * Ablauf:
 *   Chat-Message → processMessage() → Regex-Match → JobSnapshot atomisch speichern
 *   HUD liest per getSnapshot() ohne Locking.
 *
 * Heuristik / bekannte Unschärfen (aus dem Blueprint):
 *   - XP und Money werden laut Originalanalyse mit / 2.0 aufaddiert.
 *     Diese Heuristik ist hier explizit als Konstante ACCUMULATION_DIVISOR definiert.
 *   - Bekannte Job-Namen sind hartcodiert; Server-Format kann sich ändern.
 *
 * TODO: Chat-Regex an das echte OPSUCHT-Nachrichtenformat anpassen und testen.
 */
public final class JobTrackerService {

    // Divisor laut Blueprint-Beobachtung ("/ 2.0")
    private static final double ACCUMULATION_DIVISOR = 2.0;

    /**
     * Bekannte OPSUCHT-Job-Namen (aus OPMOD-Dekompilation).
     * Kleinschreibung – Vergleich erfolgt nach toLowerCase().
     */
    private static final Set<String> KNOWN_JOBS = Set.of(
        "holzfäller", "minenarbeiter", "fischer", "gräber", "jäger", "builder", "farmer"
    );

    /**
     * OPSUCHT-Chat-Format (aus OPMOD-Dekompilation bestätigt):
     *   "[JobName] • Level X • XP: Y • $Z • P%"
     *   oder Varianten davon mit Tausenderpunkt im deutschen Format.
     *
     * Zeilen mit '»' werden explizit ignoriert (Server-Formatierung
     * für Aktions-/Systemnachrichten, die kein Job-Update darstellen).
     *
     * Gruppen: 1=Level, 2=XP, 3=Money, 4=Percent
     */
    private static final Pattern JOB_PATTERN = Pattern.compile(
        "Level\\s+(\\d+)\\s*[•·]\\s*XP[:\\s]+([\\d.,]+)\\s*[•·]\\s*\\$([\\d.,]+)\\s*[•·]\\s*([\\d.,]+)%",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Erkennt den Jobnamen in einer Chat-Zeile.
     * OPSUCHT stellt den Jobnamen oft in eckigen Klammern voran: "[Holzfäller]"
     * oder nennt ihn direkt vor dem ersten " • ".
     */
    private static final Pattern JOB_NAME_PATTERN = Pattern.compile(
        "\\[(" + String.join("|",
            "Holzfäller", "Minenarbeiter", "Fischer", "Gräber", "Jäger", "Builder", "Farmer"
        ) + ")\\]",
        Pattern.CASE_INSENSITIVE
    );

    private final ConfigManager config;
    private final AtomicReference<JobSnapshot> snapshot =
        new AtomicReference<>(JobSnapshot.empty());
    private final AtomicLong trackingStartMs = new AtomicLong(0);

    // Akkumulatoren (werden NOT / 2.0 sofort geteilt, sondern beim Snapshot-Bau)
    private volatile double accXp    = 0;
    private volatile double accMoney = 0;
    private volatile String currentJob = "";

    public JobTrackerService(ConfigManager config) {
        this.config = config;
    }

    /** Wird vom Fabric-Chat-Event auf dem Client-Thread aufgerufen. */
    public void processMessage(String rawText) {
        if (!config.getConfig().enableJobTracker) return;
        // Zeilen mit '»' sind OPSUCHT-Aktions-/Systemnachrichten, kein Job-Update.
        if (rawText.contains("»")) return;

        detectJobName(rawText);
        detectJobValues(rawText);
    }

    private void detectJobName(String text) {
        Matcher m = JOB_NAME_PATTERN.matcher(text);
        if (m.find()) {
            String name = m.group(1).toLowerCase();
            if (KNOWN_JOBS.contains(name) && !name.equals(currentJob)) {
                currentJob = name;
                resetTracking();
                VisotarisLogger.debug("Job erkannt: {}", currentJob);
            }
        }
    }

    private void detectJobValues(String text) {
        Matcher m = JOB_PATTERN.matcher(text);
        if (!m.find()) return;

        int    level   = Integer.parseInt(m.group(1));
        double xp      = parseDouble(m.group(2));
        double money   = parseDouble(m.group(3));
        double percent = parseDouble(m.group(4));

        // Akkumulation mit dem dokumentierten Divisor
        accXp    += xp    / ACCUMULATION_DIVISOR;
        accMoney += money / ACCUMULATION_DIVISOR;

        long elapsedSec = trackingStartMs.get() == 0
            ? 0
            : (System.currentTimeMillis() - trackingStartMs.get()) / 1000L;

        snapshot.set(new JobSnapshot(currentJob, accXp, accMoney, level, percent, elapsedSec));
    }

    public void resetTracking() {
        trackingStartMs.set(System.currentTimeMillis());
        accXp    = 0;
        accMoney = 0;
        snapshot.set(JobSnapshot.empty());
    }

    public JobSnapshot getSnapshot() {
        return snapshot.get();
    }

    private static double parseDouble(String raw) {
        try {
            // OPSUCHT nutzt deutsches Format: Tausenderpunkt, Komma als Dezimalzeichen.
            // "1.234,56" → remove dots → "1234,56" → comma→dot → "1234.56"
            return Double.parseDouble(raw.replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
