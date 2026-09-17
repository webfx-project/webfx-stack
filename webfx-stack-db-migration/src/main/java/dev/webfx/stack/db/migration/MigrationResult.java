package dev.webfx.stack.db.migration;

import java.util.Collections;
import java.util.List;

/**
 * Summary of a migration run, for the boot log.
 *
 * @author Bruno Salmon
 */
public final class MigrationResult {

    private final int alreadyAppliedCount;
    private final List<Integer> appliedNowVersions;
    private final long durationMs;

    private MigrationResult(int alreadyAppliedCount, List<Integer> appliedNowVersions, long durationMs) {
        this.alreadyAppliedCount = alreadyAppliedCount;
        this.appliedNowVersions = appliedNowVersions;
        this.durationMs = durationMs;
    }

    public static MigrationResult upToDate(int alreadyAppliedCount) {
        return new MigrationResult(alreadyAppliedCount, Collections.emptyList(), 0);
    }

    public static MigrationResult applied(int alreadyAppliedCount, List<Integer> appliedNowVersions, long durationMs) {
        return new MigrationResult(alreadyAppliedCount, appliedNowVersions, durationMs);
    }

    public int getAlreadyAppliedCount() {
        return alreadyAppliedCount;
    }

    public List<Integer> getAppliedNowVersions() {
        return appliedNowVersions;
    }

    public long getDurationMs() {
        return durationMs;
    }

    @Override
    public String toString() {
        if (appliedNowVersions.isEmpty())
            return "database is up to date (" + alreadyAppliedCount + " scripts already applied)";
        return "applied " + appliedNowVersions.size() + " scripts " + appliedNowVersions + " in " + durationMs + "ms (" + alreadyAppliedCount + " previously applied)";
    }
}
