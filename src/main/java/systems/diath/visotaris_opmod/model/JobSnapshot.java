package systems.diath.visotaris_opmod.model;

/**
 * Snapshot des aktuellen Job-Trackerzustands.
 * Wird vom {@code JobTrackerService} aus Chat-Nachrichten abgeleitet
 * und vom HUD direkt gerendert.
 *
 * Hinweis: xp und money werden laut Blueprint mit / 2.0 aufaddiert –
 * diese Heuristik wird explizit im JobTrackerService dokumentiert
 * und ist dort zentral konfigurierbar.
 */
public final class JobSnapshot {

    /** Bekannte Job-Namen werden zentral im JobTrackerService gepflegt. */
    private final String jobName;
    private final double xp;
    private final double money;
    private final int    level;
    private final double percent;
    private final long   trackingSeconds;

    public JobSnapshot(String jobName, double xp, double money,
                       int level, double percent, long trackingSeconds) {
        this.jobName         = jobName;
        this.xp              = xp;
        this.money           = money;
        this.level           = level;
        this.percent         = percent;
        this.trackingSeconds = trackingSeconds;
    }

    /** Leerer Snapshot als Initialzustand vor dem ersten Chat-Event. */
    public static JobSnapshot empty() {
        return new JobSnapshot("", 0, 0, 0, 0, 0);
    }

    public String getJobName()        { return jobName; }
    public double getXp()             { return xp; }
    public double getMoney()          { return money; }
    public int    getLevel()          { return level; }
    public double getPercent()        { return percent; }
    public long   getTrackingSeconds(){ return trackingSeconds; }

    /** Hochrechnung auf Stundenbasis. */
    public double getXpPerHour() {
        return trackingSeconds > 0 ? (xp / trackingSeconds) * 3600.0 : 0;
    }

    public double getMoneyPerHour() {
        return trackingSeconds > 0 ? (money / trackingSeconds) * 3600.0 : 0;
    }

    @Override
    public String toString() {
        return "JobSnapshot{job='" + jobName + "', level=" + level + ", xp=" + xp + ", money=" + money + '}';
    }
}
