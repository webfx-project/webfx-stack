package dev.webfx.stack.mail.transport;

import java.time.Instant;
import java.util.Set;

/**
 * Filter for {@code listSuppressed()}: which reasons, which time window, how many entries.
 * All fields optional (null = no constraint).
 *
 * @author Bruno Salmon
 */
public final class SuppressionQuery {

    private final Set<SuppressionReason> reasons;
    private final Instant start;
    private final Instant end;
    private final Integer limit;

    public SuppressionQuery(Set<SuppressionReason> reasons, Instant start, Instant end, Integer limit) {
        this.reasons = reasons;
        this.start = start;
        this.end = end;
        this.limit = limit;
    }

    public Set<SuppressionReason> getReasons() { return reasons; }
    public Instant getStart() { return start; }
    public Instant getEnd() { return end; }
    public Integer getLimit() { return limit; }
}
