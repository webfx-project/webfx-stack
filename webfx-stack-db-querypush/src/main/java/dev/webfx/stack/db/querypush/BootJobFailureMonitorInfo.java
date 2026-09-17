package dev.webfx.stack.db.querypush;

/**
 * One application job that failed during this server task's boot, for the /monitor "Boot" drill-down —
 * the wire form of {@code dev.webfx.platform.boot.ApplicationJobFailures.Failure}. Unlike the Errors
 * card (a churning ring buffer of runtime DB errors), boot failures are a fixed snapshot taken once at
 * startup: they describe a persistent "this task didn't fully boot" condition, so the card is a
 * green/red health signal rather than a rolling log. Per server process — reflects the exact task the
 * page is connected to.
 *
 * @author Bruno Salmon
 */
public final class BootJobFailureMonitorInfo {

    private final long epochMillis;  // wall-clock time of the failure during boot
    private final String phase;      // "Initializing" / "Starting" / "Stopping"
    private final String jobName;    // the failing ApplicationJob's simple class name
    private final String message;    // the cause message (or the throwable class name when it had none)
    private final String stackTrace; // full server-side stack trace, for the drill-down (may be null)

    public BootJobFailureMonitorInfo(long epochMillis, String phase, String jobName, String message, String stackTrace) {
        this.epochMillis = epochMillis;
        this.phase = phase;
        this.jobName = jobName;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public long getEpochMillis() { return epochMillis; }
    public String getPhase() { return phase; }
    public String getJobName() { return jobName; }
    public String getMessage() { return message; }
    public String getStackTrace() { return stackTrace; }
}
