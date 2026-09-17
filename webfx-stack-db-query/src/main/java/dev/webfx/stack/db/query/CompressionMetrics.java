package dev.webfx.stack.db.query;

/**
 * Process-wide counters for QueryResult value compression (RepeatedValuesCompressor),
 * measured at the single serialization choke point in QueryResultSerialCodec.encode
 * (in the webfx-stack-db-query-serial module).
 * <p>
 * Compression runs INLINE on the Vert.x event loop, so a large result blocks every client
 * while it compresses — these counters (especially {@code maxNanos} and the over-threshold
 * {@code slowCount}) quantify that blocking risk for the /monitor page, and are the evidence
 * that would justify offloading compression to a worker thread.
 * <p>
 * Server-only in practice (encode happens server-side); the class lives in this client-safe
 * module because that is where the codec lives, but on J2CL clients it is simply never invoked.
 * Access is {@code synchronized} — a no-op on single-threaded J2CL, and it keeps the counters
 * consistent across the server's event loops on the JVM.
 *
 * @author Bruno Salmon
 */
public final class CompressionMetrics {

    /** Compressions taking at least this long are counted as slow (event-loop-block risk). */
    public static final long SLOW_COMPRESSION_NANOS = 100_000_000L; // 100 ms

    private static long count;
    private static long totalNanos;
    private static long maxNanos;
    private static long slowCount;
    private static long totalCells;

    private CompressionMetrics() {}

    /**
     * Records one compression.
     *
     * @param nanos wall time spent compressing
     * @param cells number of cells processed (values array length = rows × columns)
     */
    public static synchronized void record(long nanos, int cells) {
        count++;
        totalNanos += nanos;
        totalCells += cells;
        if (nanos > maxNanos)
            maxNanos = nanos;
        if (nanos >= SLOW_COMPRESSION_NANOS)
            slowCount++;
    }

    /** Returns an immutable snapshot of the cumulative counters (since server start). */
    public static synchronized Snapshot snapshot() {
        return new Snapshot(count, totalNanos, maxNanos, slowCount, totalCells);
    }

    /** Clears the counters so the /monitor page can measure a fresh window (Clear button). */
    public static synchronized void reset() {
        count = 0;
        totalNanos = 0;
        maxNanos = 0;
        slowCount = 0;
        totalCells = 0;
    }

    /** Cumulative compression counters. Rates are derived client-side from poll-to-poll deltas. */
    public record Snapshot(long count, long totalNanos, long maxNanos, long slowCount, long totalCells) {}
}
