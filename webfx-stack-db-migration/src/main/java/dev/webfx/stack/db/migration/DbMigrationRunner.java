package dev.webfx.stack.db.migration;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.stack.db.query.QueryArgumentBuilder;
import dev.webfx.stack.db.query.QueryService;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitArgumentBuilder;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.SubmitService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Applies the pending migration scripts through the generic SubmitService API. All pending scripts (and their
 * db_migration log rows) run in ONE transaction — a submit batch — so any failure rolls back everything and
 * the database is left exactly as the previous application version knows it. The db_migration log table is
 * created on the fly (plain SQL only, it doesn't need any domain model declaration), and concurrently booting
 * instances are serialized with a transaction-scoped advisory lock.
 *
 * @author Bruno Salmon
 */
final class DbMigrationRunner {

    private static final long ADVISORY_LOCK_KEY = 77_240_001;
    // Our DDL must fail fast rather than queue behind the live server's queries (a queued ACCESS EXCLUSIVE
    // lock would block all its subsequent readers). Also bounds the advisory-lock wait below.
    private static final String LOCK_TIMEOUT = "5s";
    // Must stay under the SubmitService provider's own SQL operation timeout (5 min in the Vert.x Postgres
    // provider), which applies to the whole batch.
    private static final String STATEMENT_TIMEOUT = "4min";
    private static final int MAX_LOCK_TIMEOUT_ATTEMPTS = 3;
    private static final long LOCK_RETRY_DELAY_MS = 10_000;
    private static final int MAX_ERROR_LENGTH = 8_000;

    // version & checksum are null on failure rows (the failing statement isn't always attributable to one
    // script); the partial unique index guarantees a version can only ever be recorded as applied once.
    private static final String CREATE_LOG_TABLE = "CREATE TABLE IF NOT EXISTS db_migration (" +
        "id bigserial PRIMARY KEY, " +
        "version integer, " +
        "file_name text NOT NULL, " +
        "checksum text, " +
        "executed_at timestamptz NOT NULL DEFAULT now(), " +
        "execution_time_ms integer, " +
        "success boolean NOT NULL, " +
        "error text)";
    private static final String CREATE_LOG_INDEX = "CREATE UNIQUE INDEX IF NOT EXISTS db_migration_success_version ON db_migration (version) WHERE success";
    private static final String LOG_TABLE_EXISTS = "SELECT to_regclass('db_migration') IS NOT NULL";
    private static final String SELECT_APPLIED = "SELECT version, checksum FROM db_migration WHERE success";
    // execution_time_ms = time elapsed since the beginning of the migration transaction (cumulative across scripts)
    private static final String INSERT_SUCCESS_ROW = "INSERT INTO db_migration (version, file_name, checksum, execution_time_ms, success) " +
        "VALUES ($1, $2, $3, (extract(epoch from clock_timestamp() - now()) * 1000)::int, true)";
    private static final String INSERT_FAILURE_ROW = "INSERT INTO db_migration (file_name, execution_time_ms, success, error) VALUES ($1, $2, false, $3)";

    private final Object dataSourceId;
    private final List<MigrationScript> scripts;

    DbMigrationRunner(Object dataSourceId, List<MigrationScript> scripts) {
        this.dataSourceId = dataSourceId;
        this.scripts = scripts;
    }

    Future<MigrationResult> run() {
        return ensureLogTable()
            .compose(v -> loadAppliedChecksumsByVersion())
            .compose(applied -> {
                List<MigrationScript> pending = computePending(applied); // throws on checksum mismatch, failing this future
                if (pending.isEmpty())
                    return Future.succeededFuture(MigrationResult.upToDate(applied.size()));
                warnIfOutOfOrder(applied, pending);
                Console.log("Applying " + pending.size() + " DB migration scripts: " + pending);
                return attemptMigration(applied.size(), pending, 1);
            });
    }

    private Future<Void> ensureLogTable() {
        // Only attempt the DDL when the table doesn't exist yet: unlike CREATE TABLE IF NOT EXISTS (which
        // just emits a notice on an existing table), CREATE INDEX IF NOT EXISTS checks table OWNERSHIP before
        // its if-not-exists short-circuit, so a less-privileged db user (e.g. a dev machine connected to a
        // shared database already migrated by the deployed server) could never even reach the no-op path.
        return queryBoolean(LOG_TABLE_EXISTS).compose(exists -> {
            if (exists)
                return Future.succeededFuture();
            return createLogTableAndIndex()
                // Two instances booting at once can (rarely) collide on the concurrent CREATE ... IF NOT EXISTS;
                // by the time we retry, the other instance has created it and IF NOT EXISTS makes this a no-op.
                .recover(e -> delay(1_000).compose(v -> createLogTableAndIndex()))
                .map(r -> null);
        });
    }

    private Future<SubmitResult> createLogTableAndIndex() {
        return submit(CREATE_LOG_TABLE)
            .compose(r -> submit(CREATE_LOG_INDEX));
    }

    private Future<Boolean> queryBoolean(String statement) {
        return QueryService.executeQuery(new QueryArgumentBuilder()
                .setDataSourceId(dataSourceId)
                .setStatement(statement)
                .build())
            .map(rs -> Boolean.TRUE.equals(rs.getValue(0, 0)));
    }

    private Future<Map<Integer, String>> loadAppliedChecksumsByVersion() {
        return QueryService.executeQuery(new QueryArgumentBuilder()
                .setDataSourceId(dataSourceId)
                .setStatement(SELECT_APPLIED)
                .build())
            .map(rs -> {
                Map<Integer, String> checksumsByVersion = new HashMap<>();
                for (int row = 0; row < rs.getRowCount(); row++)
                    checksumsByVersion.put(((Number) rs.getValue(row, 0)).intValue(), rs.getValue(row, 1));
                return checksumsByVersion;
            });
    }

    private List<MigrationScript> computePending(Map<Integer, String> appliedChecksumsByVersion) {
        List<MigrationScript> pending = new ArrayList<>();
        for (MigrationScript script : scripts) {
            String appliedChecksum = appliedChecksumsByVersion.get(script.getVersion());
            if (appliedChecksum == null)
                pending.add(script);
            else if (!appliedChecksum.equals(script.getChecksum()))
                throw new IllegalStateException(script.getFileName() + " has been modified since it was applied to this database (bundled checksum " + script.getChecksum() + " differs from applied checksum " + appliedChecksum + "). Restore the shipped script, or update the db_migration row after conscious review.");
        }
        // The database can be ahead of this build when an older build is redeployed (e.g. an emergency
        // rollback); don't block it, the deployer knows what they're doing — but warn.
        int maxBundledVersion = scripts.isEmpty() ? 0 : scripts.get(scripts.size() - 1).getVersion();
        appliedChecksumsByVersion.keySet().stream().filter(version -> version > maxBundledVersion).sorted().forEach(version ->
            Console.warn("DB migration version " + version + " is applied in the database but not bundled in this build (the database is ahead of this build)"));
        return pending;
    }

    private static void warnIfOutOfOrder(Map<Integer, String> appliedChecksumsByVersion, List<MigrationScript> pending) {
        int maxApplied = appliedChecksumsByVersion.keySet().stream().max(Integer::compare).orElse(0);
        if (pending.get(0).getVersion() < maxApplied)
            Console.warn("DB migration " + pending.get(0) + " is applied out of order (version " + maxApplied + " is already applied)");
    }

    private Future<MigrationResult> attemptMigration(int alreadyAppliedCount, List<MigrationScript> pending, int attempt) {
        long t0 = System.currentTimeMillis();
        return SubmitService.executeSubmitBatch(buildTransactionBatch(pending))
            .map(b -> MigrationResult.applied(alreadyAppliedCount, versionsOf(pending), System.currentTimeMillis() - t0))
            .recover(error -> handleMigrationFailure(alreadyAppliedCount, pending, attempt, error, System.currentTimeMillis() - t0));
    }

    private Batch<SubmitArgument> buildTransactionBatch(List<MigrationScript> pending) {
        List<SubmitArgument> args = new ArrayList<>();
        // The submit batch runs in a single transaction, so these SET LOCAL apply to all statements below
        args.add(submitArgument("SET LOCAL lock_timeout = '" + LOCK_TIMEOUT + "'"));
        args.add(submitArgument("SET LOCAL statement_timeout = '" + STATEMENT_TIMEOUT + "'"));
        // Serializes concurrently booting instances (the lock is released on commit/rollback)
        args.add(submitArgument("SELECT pg_advisory_xact_lock(" + ADVISORY_LOCK_KEY + ")"));
        for (MigrationScript script : pending) {
            for (String statement : script.getStatements())
                args.add(submitArgument(statement));
            args.add(submitArgument(INSERT_SUCCESS_ROW, script.getVersion(), script.getFileName(), script.getChecksum()));
        }
        return new Batch<>(args.toArray(new SubmitArgument[0]));
    }

    private Future<MigrationResult> handleMigrationFailure(int alreadyAppliedCount, List<MigrationScript> pending, int attempt, Throwable error, long elapsedMs) {
        // The batch transaction has already been rolled back by the SubmitService provider. A concurrently
        // booting instance may have applied the scripts while we were waiting on the advisory lock (this
        // typically surfaces as a duplicate key error on db_migration_success_version, as the batch API
        // doesn't allow re-checking the pending scripts under the lock) — so re-read the log to find out.
        return loadAppliedChecksumsByVersion()
            .recover(e -> Future.succeededFuture(Collections.emptyMap())) // best effort — keep the original error otherwise
            .compose(applied -> {
                if (applied.keySet().containsAll(versionsOf(pending))) {
                    Console.log("DB migrations were applied concurrently by another booting instance");
                    return Future.succeededFuture(MigrationResult.upToDate(applied.size()));
                }
                if (isLockTimeout(error) && attempt < MAX_LOCK_TIMEOUT_ATTEMPTS) {
                    Console.warn("DB migration attempt " + attempt + " hit a lock timeout (the database was busy); retrying in " + LOCK_RETRY_DELAY_MS / 1_000 + "s...");
                    return delay(LOCK_RETRY_DELAY_MS).compose(v -> attemptMigration(alreadyAppliedCount, pending, attempt + 1));
                }
                return recordFailure(pending, error, elapsedMs)
                    .compose(v -> Future.failedFuture(error));
            });
    }

    private Future<Void> recordFailure(List<MigrationScript> pending, Throwable error, long elapsedMs) {
        String attemptedFileNames = pending.stream().map(MigrationScript::getFileName).collect(Collectors.joining(", "));
        String message = error.getMessage() == null ? error.toString() : error.getMessage();
        if (message.length() > MAX_ERROR_LENGTH)
            message = message.substring(0, MAX_ERROR_LENGTH);
        return submit(INSERT_FAILURE_ROW, "attempted: " + attemptedFileNames, (int) elapsedMs, message)
            .<Void>map(r -> null)
            .recover(e -> {
                Console.error("Could not record the DB migration failure in the db_migration table", e);
                return Future.succeededFuture();
            });
    }

    private Future<SubmitResult> submit(String statement, Object... parameters) {
        return SubmitService.executeSubmit(submitArgument(statement, parameters));
    }

    private SubmitArgument submitArgument(String statement, Object... parameters) {
        SubmitArgumentBuilder builder = new SubmitArgumentBuilder()
            .setDataSourceId(dataSourceId)
            .setStatement(statement); // raw SQL (no language set = not DQL, passed through untouched)
        if (parameters.length > 0)
            builder.setParameters(parameters);
        return builder.build();
    }

    private static List<Integer> versionsOf(List<MigrationScript> scripts) {
        return scripts.stream().map(MigrationScript::getVersion).collect(Collectors.toList());
    }

    private static boolean isLockTimeout(Throwable error) {
        for (Throwable e = error; e != null; e = e.getCause() == e ? null : e.getCause()) {
            String message = e.getMessage();
            if (message != null && message.contains("lock timeout")) // PostgreSQL: "canceling statement due to lock timeout"
                return true;
        }
        return false;
    }

    private static Future<Void> delay(long delayMs) {
        Promise<Void> promise = Promise.promise();
        Scheduler.scheduleDelay(delayMs, promise::complete);
        return promise.future();
    }
}
