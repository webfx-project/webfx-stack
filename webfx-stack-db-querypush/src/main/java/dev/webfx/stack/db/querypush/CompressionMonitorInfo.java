package dev.webfx.stack.db.querypush;

/**
 * QueryResult compression metrics for the /monitor page — the wire form of
 * {@code CompressionMetrics.Snapshot}. Compression runs inline on the Vert.x event loop, so
 * {@code maxNanos} and {@code slowCount} (compressions over the slow threshold) quantify its
 * event-loop-blocking risk. Rates are derived client-side from poll-to-poll deltas.
 *
 * @author Bruno Salmon
 */
public final class CompressionMonitorInfo {

    private final long count;       // total compressions (since server start)
    private final long totalNanos;  // total compression wall time
    private final long maxNanos;    // slowest single compression
    private final long slowCount;   // compressions over the slow threshold (event-loop-block risk)
    private final long totalCells;  // total cells compressed (rows × columns)

    public CompressionMonitorInfo(long count, long totalNanos, long maxNanos, long slowCount, long totalCells) {
        this.count = count;
        this.totalNanos = totalNanos;
        this.maxNanos = maxNanos;
        this.slowCount = slowCount;
        this.totalCells = totalCells;
    }

    public long getCount() { return count; }
    public long getTotalNanos() { return totalNanos; }
    public long getMaxNanos() { return maxNanos; }
    public long getSlowCount() { return slowCount; }
    public long getTotalCells() { return totalCells; }
}
