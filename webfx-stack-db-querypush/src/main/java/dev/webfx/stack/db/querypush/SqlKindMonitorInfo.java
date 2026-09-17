package dev.webfx.stack.db.querypush;

/**
 * SQL execution metrics for one kind (reads = queries, or writes = submits), part of
 * {@link SqlExecutionMonitorInfo}. Cumulative counters plus live queue gauges; rates are
 * derived client-side from poll-to-poll deltas.
 *
 * @author Bruno Salmon
 */
public final class SqlKindMonitorInfo {

    private final long count;          // total operations executed (since server start)
    private final long totalNanos;     // total execution wall time
    private final long errorCount;     // operations that failed
    private final int waiting;         // operations currently queued (not yet started)
    private final int executing;       // operations currently executing
    private final int maxConcurrency;  // concurrency cap (= pool size)
    private final int peakWaiting;     // high-water mark of `waiting`
    private final int shed;            // total shedWhenBusy operations rejected at admission (since server start)

    public SqlKindMonitorInfo(long count, long totalNanos, long errorCount, int waiting, int executing, int maxConcurrency, int peakWaiting, int shed) {
        this.count = count;
        this.totalNanos = totalNanos;
        this.errorCount = errorCount;
        this.waiting = waiting;
        this.executing = executing;
        this.maxConcurrency = maxConcurrency;
        this.peakWaiting = peakWaiting;
        this.shed = shed;
    }

    public long getCount() { return count; }
    public long getTotalNanos() { return totalNanos; }
    public long getErrorCount() { return errorCount; }
    public int getWaiting() { return waiting; }
    public int getExecuting() { return executing; }
    public int getMaxConcurrency() { return maxConcurrency; }
    public int getPeakWaiting() { return peakWaiting; }
    public int getShed() { return shed; }
}
