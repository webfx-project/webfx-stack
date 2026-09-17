package dev.webfx.stack.db.querysubmit;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.util.AsyncQueue;
import dev.webfx.platform.shutdown.Shutdown;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.vertx.VertxInstance;
import dev.webfx.stack.db.datasource.ConnectionDetails;
import dev.webfx.stack.db.datasource.LocalDataSource;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.SqlAnalyzeRegistry;
import dev.webfx.stack.db.query.SqlExecutionMonitor;
import dev.webfx.stack.db.query.spi.QueryServiceProvider;
import dev.webfx.stack.db.submit.GeneratedKeyReference;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.listener.SubmitListenerService;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import dev.webfx.stack.session.state.AuditActorRegistry;
import dev.webfx.stack.session.state.RestrictedPrincipalRegistry;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.session.state.TransactionPreambleRegistry;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.webfx.platform.util.vertx.VertxAsync.toVertxFuture;
import static dev.webfx.platform.util.vertx.VertxAsync.toWebfxFuture;
import static dev.webfx.stack.db.querysubmit.VertxSqlUtil.*;

/**
 * Note: the same class is used for both the QueryService and the SubmitService, so there will be 2 different instances,
 * one for each service.
 *
 * @author Bruno Salmon
 */
public class VertxLocalPostgresQuerySubmitServiceProvider implements QueryServiceProvider, SubmitServiceProvider {

    private static final int QUERY_POOL_SIZE = 20;
    private static final int SUBMIT_POOL_SIZE = 10;
    private static final boolean LOG_TIMINGS = true;
    private static final long SQL_OPERATION_WARNING_MILLIS = 5_000;
    private static final long SQL_OPERATION_TIMEOUT_MILLIS = 5 * 60 * 1000; // 5-minute timeout for SQL operations

    private final AsyncQueue asyncQueue;
    private final Pool pool;
    private final SqlExecutionMonitor.Kind monitorKind; // READ for queries, WRITE for submits

    public VertxLocalPostgresQuerySubmitServiceProvider(LocalDataSource localDataSource, boolean submit) {
        ConnectionDetails cd = localDataSource.getLocalConnectionDetails();
        // The pool size (= max parallel SQL operations, as the AsyncQueue cap is kept 1:1 with the
        // connection pool max) can be tuned per environment through the datasource configuration;
        // when unset (-1) the historical defaults apply.
        int configuredPoolSize = submit ? cd.getSubmitPoolSize() : cd.getQueryPoolSize();
        int poolSize = configuredPoolSize > 0 ? configuredPoolSize : (submit ? SUBMIT_POOL_SIZE : QUERY_POOL_SIZE);
        asyncQueue = new AsyncQueue(poolSize, "POSTGRES-" + (submit ? "SUBMIT" : "QUERY"))
            .setExecutionTimeout(SQL_OPERATION_TIMEOUT_MILLIS);
        // Logged AFTER the queue is built — this.log() delegates to asyncQueue.log()
        log("Pool size = " + poolSize + (configuredPoolSize > 0 ? " (from configuration)" : " (default)"));
        // Register this queue for /monitor so the snapshot can read its live depth, and remember
        // the kind so completed operations are counted as reads (queries) or writes (submits).
        monitorKind = submit ? SqlExecutionMonitor.Kind.WRITE : SqlExecutionMonitor.Kind.READ;
        SqlExecutionMonitor.get().registerQueue(monitorKind, asyncQueue);

        PgConnectOptions connectOptions = new PgConnectOptions()
            .setPort(cd.getPort())
            .setHost(cd.getHost())
            .setDatabase(cd.getDatabaseName())
            .setUser(cd.getUsername())
            .setPassword(cd.getPassword());
        // Use TLS when the server offers it: AWS RDS enforces rds.force_ssl=1 so the connection must
        // be encrypted, while PREFER still falls back to plaintext for servers that don't support SSL
        // (e.g. local dev / the current LiquidWeb host) — so this is a safe universal default.
        connectOptions.setSslMode(SslMode.PREFER);
        // RDS's server certificate is signed by the Amazon RDS CA, which isn't in the JVM's default
        // trust store. Without trust config, Vert.x 5 fails cert verification and PREFER silently falls
        // back to plaintext (which RDS then rejects with "no encryption"). trustAll keeps the connection
        // encrypted without verifying the chain (≈ sslmode=require). To harden to full verification,
        // replace setTrustAll(true) with setTrustOptions(new PemTrustOptions().addCertPath("rds-ca.pem"))
        // and SslMode.VERIFY_FULL.
        connectOptions.setSslOptions(new ClientSSLOptions().setTrustAll(true));

        // Pool Options
        PoolOptions poolOptions = new PoolOptions()
            .setMaxSize(poolSize)
            .setIdleTimeout(30) // We release the connection after 30 min of inactivity (especially for remote databases)
            .setIdleTimeoutUnit(TimeUnit.MINUTES);

        // Create the pool from the data object
        log("Creating pool on server start");
        pool = PgBuilder.pool()
            .with(poolOptions)
            .connectingTo(connectOptions)
            .using(VertxInstance.getVertx())
            .build();

        // Closing properly the poll on the server shutdown
        Shutdown.addShutdownHook(e -> {
            log("Closing pool on server shutdown");
            pool.close();
        });
    }

    @Override
    public Future<QueryResult> executeQuery(QueryArgument argument) {
        Object runId = ThreadLocalStateHolder.getRunId();
        Boolean backoffice = callerBackoffice(runId);
        // shedWhenBusy => the queue rejects fast ("ServerBusy" error) instead of queueing when at
        // capacity — the caller marked this query as an optional revalidation with a cached fallback.
        return asyncQueue.addAsyncOperation(argument, argument.getPriority(), coalescingKey(argument, runId), arg -> executeQueryNow(arg, backoffice), argument.isShedWhenBusy());
    }

    /**
     * Build the AsyncQueue coalescing key from the caller's callId and the client's runId.
     * Returns {@code null} when either is missing — bare callIds could collide across clients
     * on the shared server-side queue, so unknown clients get no coalescing rather than wrong
     * coalescing.
     */
    private static Object coalescingKey(QueryArgument argument, Object runId) {
        int callId = argument.getCallId();
        if (callId == 0 || runId == null)
            return null;
        return runId + ":" + callId;
    }

    /**
     * The caller's origin for the /monitor BO/FO breakdown: TRUE = back-office, FALSE = front-office,
     * null = unknown (no caller — e.g. a server-internal push re-fire, which has no runId). Read on the
     * CALLER thread at the service entry — the thread-local state is gone by the time the async SQL
     * execution runs. Reliable for the React clients (a back-office client sends the backoffice flag on
     * every message; a front-office client never does).
     */
    private static Boolean callerBackoffice(Object runId) {
        return runId == null ? null : ThreadLocalStateHolder.isBackoffice();
    }

    /**
     * Records a completed operation in the /monitor SqlExecutionMonitor: always the counter, plus — on
     * failure — the error detail (statement + cause message + caller origin) for the "Errors"
     * drill-down. {@code statement} is the operation's statement (for a batch, its first, as the
     * representative; the cause message carries the specific failure).
     */
    private void recordCompletion(long t0n, AsyncResult<?> ar, String statement, Boolean backoffice) {
        SqlExecutionMonitor mon = SqlExecutionMonitor.get();
        mon.record(monitorKind, System.nanoTime() - t0n, ar.succeeded());
        if (ar.failed())
            mon.recordError(monitorKind, statement, errorMessage(ar.cause()), backoffice, System.currentTimeMillis());
    }

    /** A concise message for a failed operation's cause (null-safe; falls back to the type name). */
    private static String errorMessage(Throwable cause) {
        if (cause == null)
            return "(unknown error)";
        String msg = cause.getMessage();
        return msg != null ? msg : cause.toString();
    }

    /** The batch's first statement, as the representative statement for a failed batch (null if empty). */
    private static String firstStatementOf(QueryArgument[] arr) {
        return arr != null && arr.length > 0 ? arr[0].getStatement() : null;
    }

    private static String firstStatementOf(SubmitArgument[] arr) {
        return arr != null && arr.length > 0 ? arr[0].getStatement() : null;
    }

    private Future<QueryResult> executeQueryNow(QueryArgument argument, Boolean backoffice) {
        long t0 = System.currentTimeMillis();
        long t0n = System.nanoTime();
        return toWebfxFuture(withConnection(pool, connection -> executeConnectionQuery(connection, argument, backoffice)))
            .onFailure(e -> log("⛔️ ERROR with executeQuery(" + argument + "): " + e.getMessage()))
            .onSuccess(x -> { // Just for time report
                if (LOG_TIMINGS) {
                    long executionTimeMillis = System.currentTimeMillis() - t0;
                    log((executionTimeMillis < SQL_OPERATION_WARNING_MILLIS ? "" : "⚠️ WARNING: ") + "Query executed in " + executionTimeMillis + "ms: " + argument);
                }
            })
            .onComplete(ar -> recordCompletion(t0n, ar, argument.getStatement(), backoffice));
    }

    @Override
    public Future<Batch<QueryResult>> executeQueryBatch(Batch<QueryArgument> batch) {
        // Use the first argument's priority/callId as the batch metadata. By convention all
        // QueryArguments inside a batch originate from the same caller so they share the same
        // priority and source.
        QueryArgument[] arr = batch.getArray();
        int priority = arr.length > 0 ? arr[0].getPriority() : QueryArgument.STANDARD_PRIORITY;
        Object runId = ThreadLocalStateHolder.getRunId();
        Object key = arr.length > 0 ? coalescingKey(arr[0], runId) : null;
        Boolean backoffice = callerBackoffice(runId);
        return asyncQueue.addAsyncOperation(batch, priority, key, b -> executeQueryBatchNow(b, backoffice));
    }

    private Future<Batch<QueryResult>> executeQueryBatchNow(Batch<QueryArgument> batch, Boolean backoffice) {
        long t0 = System.currentTimeMillis();
        long t0n = System.nanoTime();
        return toWebfxFuture(withConnection(pool, connection ->
            toVertxFuture(batch.executeSerial(QueryResult[]::new, arg ->
                toWebfxFuture(executeConnectionQuery(connection, arg, backoffice))))
        ))
            .onFailure(e -> log("⛔️ ERROR with executeQueryBatch(" + batch + "): " + e.getMessage()))
            .onSuccess(x -> { // Just for time report
                if (LOG_TIMINGS) {
                    long executionTimeMillis = System.currentTimeMillis() - t0;
                    log((executionTimeMillis < SQL_OPERATION_WARNING_MILLIS ? "" : "⚠️ WARNING: ") + "Query batch executed in " + executionTimeMillis + "ms");
                }
            })
            .onComplete(ar -> recordCompletion(t0n, ar, firstStatementOf(batch.getArray()), backoffice));
    }

    private io.vertx.core.Future<QueryResult> executeConnectionQuery(SqlConnection connection, QueryArgument argument, Boolean backoffice) {
        // Register this actual SQL execution in the monitor (statement + cancel handle + caller origin),
        // for the /monitor in-flight list + per-statement rollup; deregister on completion.
        long monId = SqlExecutionMonitor.get().onStart(monitorKind, argument.getStatement(), pgCancelHandle(connection), backoffice);
        // If an admin armed this statement for analyze, capture its real params and EXPLAIN it
        // (separate connection — the real query below is untouched). Reads only, by construction:
        // this is the query path (monitorKind == READ); the submit path never analyzes.
        maybeAnalyzeQuery(argument);
        return connection
            .preparedQuery(argument.getStatement())
            .execute(tupleFromArguments(argument.getParameters()))
            .onComplete(ar -> SqlExecutionMonitor.get().onEnd(monId))
            .map(rs -> {
                QueryResult result = VertxSqlUtil.toWebFxQueryResult(rs);
                // Echo the caller's fire sequence so the client can discard stale results when
                // multiple fires from the same source race on the network.
                result.setCallSeq(argument.getCallSeq());
                return result;
            });
    }

    /**
     * If {@code argument}'s statement was armed for analysis (via {@link SqlAnalyzeRegistry}), run
     * {@code EXPLAIN (ANALYZE, BUFFERS)} on it with these real parameters on a fresh pool connection
     * and store the plan for the /monitor poll to collect. Fire-and-forget: it never affects the
     * real query. {@code EXPLAIN ANALYZE} executes the query, but this path is reads-only so it has
     * no side effects (one extra run of the query, admin-initiated and one-shot). The statement is
     * the server's own — the arm only matches statements the server actually runs — so this never
     * runs arbitrary SQL.
     */
    private void maybeAnalyzeQuery(QueryArgument argument) {
        SqlAnalyzeRegistry registry = SqlAnalyzeRegistry.get();
        if (!registry.hasArmed()) // hot-path guard: no map lookup when nothing is armed
            return;
        String statement = argument.getStatement();
        if (!registry.claimIfArmed(statement, System.currentTimeMillis()))
            return;
        Object[] parameters = argument.getParameters();
        // Captured and shown to whoever reads the analyze results, which records the parameters of
        // whoever ran the statement next - so the values here are somebody else's data. Count only.
        String parametersDisplay = parameters == null || parameters.length == 0 ? null : QueryArgument.describeParameters(parameters);
        // The original DQL this SQL was compiled from (server-side only, via the transient
        // original-argument chain the DQL interceptor sets); null when the query wasn't DQL-derived.
        String dql = dqlStatementOf(argument);
        // The client build version of the caller that ran THIS occurrence — read now (the thread-local
        // caller state won't survive the async EXPLAIN below). Null for an older client or a
        // server-internal run (e.g. a push refresh with no client caller). Lets the admin tell whether
        // a still-slow query is coming from an old client (temporary, until rollout) or the latest one.
        String clientVersion = ThreadLocalStateHolder.getClientVersion();
        withConnection(pool, c -> c.preparedQuery("EXPLAIN (ANALYZE, BUFFERS) " + statement).execute(tupleFromArguments(parameters)))
            .onComplete(ar -> {
                String plan;
                if (ar.succeeded()) {
                    StringBuilder sb = new StringBuilder();
                    for (Row row : ar.result())
                        sb.append(row.getString(0)).append('\n');
                    plan = sb.toString();
                } else {
                    plan = "EXPLAIN failed: " + ar.cause();
                    log("⚠️ WARNING: analyze EXPLAIN failed for statement: " + statement + " — " + ar.cause());
                }
                registry.storeResult(statement, dql, parametersDisplay, plan, clientVersion, System.currentTimeMillis());
            });
    }

    /** Walks the argument's original-argument chain to the DQL statement it was compiled from, or null. */
    private static String dqlStatementOf(QueryArgument argument) {
        if (argument == null)
            return null;
        if ("DQL".equalsIgnoreCase(argument.getLanguage()))
            return argument.getStatement();
        return dqlStatementOf(argument.getOriginalArgument());
    }

    /**
     * Best-effort cancel action for an executing statement, stored opaquely by the monitor as the
     * in-flight query's cancel handle. We return a client-safe {@link Runnable} (not the
     * PgConnection) so the monitor module that triggers cancellation stays free of any Vert.x
     * pg-client dependency. {@code PgConnection.cancelRequest()} is out-of-band — it opens a fresh
     * connection and asks PostgreSQL to cancel the query running on this backend; it is advisory
     * (the query may finish first) and a cancelled query fails with SQLSTATE 57014, completing its
     * future exceptionally (so {@code onEnd} still runs). Returns null for non-PG connections.
     */
    private Runnable pgCancelHandle(SqlConnection connection) {
        try {
            io.vertx.pgclient.PgConnection pg = io.vertx.pgclient.PgConnection.cast(connection);
            return () -> pg.cancelRequest().onFailure(t -> log("⚠️ WARNING: query cancelRequest failed: " + t));
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Refuses the write when the caller's session is only allowed to read.
     *
     * <p>Checked here, at the last point before SQL, because this is the only place every write
     * converges: client DML, the document service, a credential change. Anywhere higher and the
     * check would have to be repeated per feature, and the one that got missed would be the hole.
     *
     * <p>Read on the calling thread, never inside the async queue — the queue may run the operation
     * later and on another thread, where the thread-local state is gone. Same reason {@code
     * backoffice} and the audit note are captured where they are.
     *
     * <p>The message carries a stable marker the client can recognise without this layer knowing
     * anything about what kind of session is restricted or why.
     */
    private static Future<Void> checkWriteAllowed() {
        if (RestrictedPrincipalRegistry.isCurrentUserRestricted())
            return Future.failedFuture("[ReadOnlySessionError] This session is not allowed to modify data");
        return Future.succeededFuture();
    }


    @Override
    public Future<SubmitResult> executeSubmit(SubmitArgument argument) {
        // Submits get priority but no source-based coalescing (sourceId=null): cancelling a pending
        // submit because a newer same-source one arrived would drop side-effecting work.
        Boolean backoffice = callerBackoffice(ThreadLocalStateHolder.getRunId());
        // A preamble on its own opens a transaction that then does nothing, and a lone submit is its own
        // transaction, so it cannot be the preamble for anything else. Asking for one here means the
        // caller believes it is inside a batch when it is not — refuse rather than run the write without
        // the preamble it thinks it has.
        if (argument.isTransactionPreamble())
            return Future.failedFuture("[TransactionPreambleError] A transaction preamble is only meaningful inside a submit batch");
        return checkWriteAllowed().compose(ignored ->
            asyncQueue.addAsyncOperation(argument, argument.getPriority(), null, arg -> executeSubmitNow(arg, backoffice)));
    }

    private Future<SubmitResult> executeSubmitNow(SubmitArgument argument, Boolean backoffice) {
        // Note: executeIndividualSubmitWithConnection() already logs failure and timing
        long t0n = System.nanoTime();
        return toWebfxFuture(withConnection(pool, connection -> executeIndividualSubmitWithConnection(argument, connection, null, backoffice)))
            .onComplete(ar -> recordCompletion(t0n, ar, argument.getStatement(), backoffice));
    }

    @Override
    public Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch) {
        // Same convention as executeQueryBatch: use the first arg's priority. No source coalescing
        // on submits — see executeSubmit() for the rationale.
        SubmitArgument[] arr = batch.getArray();
        int priority = arr.length > 0 ? arr[0].getPriority() : SubmitArgument.STANDARD_PRIORITY;
        Boolean backoffice = callerBackoffice(ThreadLocalStateHolder.getRunId());
        // Read here, NOT inside executeSubmitBatchNow: the async queue may run that later and on
        // another thread, where the thread-local state is gone. Same reason backoffice is captured
        // on this line rather than at execution time.
        String auditNote = callerAuditNote();
        Object auditActorId = AuditActorRegistry.currentActorId();
        Batch<SubmitArgument> resolvedBatch;
        try {
            resolvedBatch = resolveTransactionPreamble(batch);
        } catch (IllegalStateException e) {
            return Future.failedFuture(e.getMessage());
        }
        return checkWriteAllowed().compose(ignored ->
            asyncQueue.addAsyncOperation(resolvedBatch, priority, null, b -> executeSubmitBatchNow(b, backoffice, auditNote, auditActorId)));
    }

    /**
     * Replaces every transaction-preamble request in the batch with the SQL the application registered.
     *
     * <p>A client sets {@link SubmitArgument#isTransactionPreamble()} on an entry to say its
     * transaction needs a preamble; what that preamble SAYS is decided here, from
     * {@link TransactionPreambleRegistry}, never from anything the client sent. The statement the client
     * put in the entry is discarded — that is the whole point, since sending it was how a caller used to
     * choose the privileges its own transaction ran with.
     *
     * <p>Resolved on the calling thread, with the caller's state still in place, for the same reason
     * {@code backoffice} and the audit note are captured where they are.
     *
     * <p>Refuses rather than drops when nothing is registered. Dropping would run the write with no
     * preamble at all, which either fails deep in a trigger with a message about a missing setting, or —
     * worse — succeeds under whatever the triggers assume when nobody said. Neither is something to
     * discover in production, so a batch that asks for a preamble nobody can supply does not run.
     */
    private static Batch<SubmitArgument> resolveTransactionPreamble(Batch<SubmitArgument> batch) {
        SubmitArgument[] arr = batch.getArray();
        SubmitArgument[] resolved = null; // stays null (and the batch untouched) when no entry asks
        for (int i = 0; i < arr.length; i++) {
            SubmitArgument argument = arr[i];
            if (argument == null || !argument.isTransactionPreamble())
                continue;
            String preambleSql = TransactionPreambleRegistry.currentPreambleStatement();
            if (preambleSql == null)
                throw new IllegalStateException("[TransactionPreambleError] This submit asks for a transaction preamble, but no application resolver supplied one");
            if (resolved == null)
                resolved = arr.clone();
            resolved[i] = SubmitArgument.builder().copy(argument)
                    .setTransactionPreamble(false) // resolved now — downstream sees an ordinary statement
                    .setLanguage(null)             // plain SQL, so nothing tries to translate it
                    .setStatement(preambleSql)
                    .setParameters((Object[]) null) // a request carries no parameters; the resolver's SQL is self-contained
                    .build();
        }
        return resolved == null ? batch : new Batch<>(resolved);
    }

    /**
     * Who is making this change, for the database audit triggers to record.
     *
     * The person/account audit tables (person_account_move, person_link_change) read
     * `current_setting('kbs.audit_note', true)`, which until now only maintenance scripts ever
     * set — so every change made through the app was attributed to the database ROLE, and a
     * trail meant to answer "who did this" could only answer "the server did". Stamping the
     * authenticated principal onto the transaction closes that.
     *
     * Returns null when nobody is authenticated (guests, server jobs), which leaves the note
     * unset exactly as before rather than inventing an actor.
     */
    private static String callerAuditNote() {
        Object userId = ThreadLocalStateHolder.getUserId();
        if (userId == null)
            return null;
        // The principal defines its own short form (Modality's prints person=..,account=..), which
        // keeps this layer free of any knowledge of what a user IS. A principal that forgets to
        // override toString() would write Object@1a2b3c into the trail — which is exactly what
        // ModalityUserPrincipal.toString() exists to prevent.
        return "user:" + userId;
    }

    /**
     * Applies the audit note to THIS transaction, before any of the batch runs.
     *
     * `set_config(..., true)` is the function form of SET LOCAL: transaction-scoped, so it cannot
     * leak onto the next borrower of a pooled connection. Parameterised rather than interpolated,
     * so a principal can never be read as SQL.
     */
    private static io.vertx.core.Future<?> applyAuditNote(SqlConnection connection, String auditNote, Object auditActorId) {
        if (auditNote == null && auditActorId == null)
            return io.vertx.core.Future.succeededFuture();
        // Both settings in one statement, so identifying the actor costs one round-trip, not two.
        // The id is what the audit tables store in changed_by_person_id; the note stays as the
        // frozen text beside it, the way History keeps username next to userPerson.
        return connection.preparedQuery(
                "select set_config('kbs.audit_note', $1, true), set_config('kbs.audit_person_id', $2, true)")
            .execute(Tuple.of(auditNote, auditActorId == null ? null : String.valueOf(auditActorId)));
    }

    private Future<Batch<SubmitResult>> executeSubmitBatchNow(Batch<SubmitArgument> batch, Boolean backoffice, String auditNote, Object auditActorId) {
        // This batch may use GeneratedKeyReference instances in its parameters, which we will need to resolve during
        // the execution. To do so, we create batchIndexGeneratedKeys, which is a list of generated keys for each
        // SubmitArgument in the batch (index 0 will contain the possible generated keys from the execution of the first
        // SubmitArgument, index 1 from the second, etc...)
        List<Object[]> batchIndexGeneratedKeys = new ArrayList<>(batch.getArray().length);
        long t0 = System.currentTimeMillis();
        long t0n = System.nanoTime();
        // The statement that actually failed, so the /monitor "Errors" drill-down shows the offending
        // DML instead of the batch's first statement (the transaction opener, e.g.
        // "select set_transaction_parameters(...)"). executeSerial runs the batch one statement at a
        // time and stops at the first failure, so this is set exactly once, on the culprit. Single
        // element holder because it's assigned from the serial-execution lambda below.
        final String[] failingStatement = { null };
        // We embed the batch execution inside a transaction using Vert.x API (and convert the return Vert.x Future<SubmitResult> into WebFX Future<SubmitResult>)
        return toWebfxFuture(withTransaction(pool, connection ->
            // Stamp the actor onto the transaction first, so every audit trigger the batch fires
            // records who did it. One extra round-trip per submit batch, and only when someone is
            // authenticated — submits are far rarer than queries.
            applyAuditNote(connection, auditNote, auditActorId).compose(ignored ->
            // We execute the batch in a serial order (we need a couple of Vert.x <-> WebFX Future for that)
            toVertxFuture(batch.executeSerial(SubmitResult[]::new, arg -> toWebfxFuture(
                // We execute this individual submission, passing batchIndexGeneratedKeys (for GeneratedKeyReference resolution)
                executeIndividualSubmitWithConnection(arg, connection, batchIndexGeneratedKeys, backoffice)
                    // Remember the first statement that fails — the one that actually raised the error —
                    // so recordCompletion records it rather than the batch's transaction opener.
                    .onFailure(e -> { if (failingStatement[0] == null) failingStatement[0] = arg.getStatement(); })
                    .map(submitResult -> { // Identity mapping, just for batchIndexGeneratedKeys management
                        // We collect the possible generated keys (if the last submission was "insert ... returning id")
                        batchIndexGeneratedKeys.add(submitResult.getGeneratedKeys());
                        return submitResult;
                    })
            ))))
        )).onSuccess(x -> { // Just for time report
            if (LOG_TIMINGS) {
                long executionTimeMillis = System.currentTimeMillis() - t0;
                log((executionTimeMillis < SQL_OPERATION_WARNING_MILLIS ? "" : "⚠️ WARNING: ") + "Submit batch executed in " + executionTimeMillis + "ms");
            }
            onSuccessfulSubmitBatch(batch);
        }).onComplete(ar -> recordCompletion(t0n, ar,
            failingStatement[0] != null ? failingStatement[0] : firstStatementOf(batch.getArray()), backoffice));
    }

    private static void onSuccessfulSubmitBatch(Batch<SubmitArgument> batch) {
        SubmitListenerService.fireSuccessfulSubmit(batch.getArray());
    }


    private io.vertx.core.Future<SubmitResult> executeIndividualSubmitWithConnection(SubmitArgument argument, SqlConnection connection, List<Object[]> batchIndexGeneratedKeys, Boolean backoffice) {
        // Register this SQL execution in the monitor (statement + cancel handle + caller origin); deregister on completion.
        long monId = SqlExecutionMonitor.get().onStart(monitorKind, argument.getStatement(), pgCancelHandle(connection), backoffice);
        // We get a prepared query from the connection
        PreparedQuery<RowSet<Row>> preparedQuery = connection
            .preparedQuery(argument.getStatement()); // statement can be insert, update or delete
        // We will execute the query with either a Tuple (1 row of parameters) or a List<Tuple> (several rows of parameters)
        io.vertx.core.Future<RowSet<Row>> queryExecutionFuture; // Will contain the Vert.x Future of that execution
        Object[] parameters = argument.getParameters();
        // In case there are several rows of parameters (i.e., batch of parameters)
        if (Arrays.length(parameters) == 1 && parameters[0] instanceof Batch) {
            // We get the rows of parameters from the batch. The returned array Object[] represents rows, and each row
            // contains the parameters of that row (so it's also an array of Object[])
            Object[] parametersRows = ((Batch<?>) parameters[0]).getArray();
            // For each row, we replace the GeneratedKeyReference instances with their actual generated keys (should be known as this stage)
            Arrays.forEach(parametersRows, row -> replaceGeneratedKeyReferencesWithActualGeneratedKeys((Object[]) row, batchIndexGeneratedKeys));
            // We map the row array into a Vert.x Tuple array
            Tuple[] tuples = Arrays.map(parametersRows, params -> tupleFromArguments((Object[]) params), Tuple[]::new);
            // And finally execute that batch (passing the Tuples as a list)
            queryExecutionFuture = preparedQuery.executeBatch(Arrays.asList(tuples));
        } else { // Case a single row of parameters
            // We replace the GeneratedKeyReference instances with their actual generated keys (should be known as this stage)
            replaceGeneratedKeyReferencesWithActualGeneratedKeys(parameters, batchIndexGeneratedKeys);
            // We execute that query (passing the arguments as a Vert.x Tuple)
            queryExecutionFuture = preparedQuery.execute(tupleFromArguments(parameters));
        }
        // Waiting for the completion of the previous query execution
        long t0 = System.currentTimeMillis();
        return queryExecutionFuture
            .onComplete(ar -> SqlExecutionMonitor.get().onEnd(monId))
            .onFailure(e -> log("⛔️ ERROR with executeIndividualSubmitWithConnection(" + argument + "): " + e.getMessage()))
            .map(rs -> { // on success, returns rs as a Vert.x RowSet<Row>
                if (LOG_TIMINGS) {
                    long executionTimeMillis = System.currentTimeMillis() - t0;
                    log((executionTimeMillis < SQL_OPERATION_WARNING_MILLIS ? "" : "⚠️ WARNING: ") + "Submit executed in " + executionTimeMillis + "ms: " + argument);
                }
                onSuccessfulSubmit(argument);
                // We convert that Vert.x RowSet into a WebFX SubmitResult
                return toWebFxSubmitResult(rs, argument);
            });
    }

    private static void onSuccessfulSubmit(SubmitArgument argument) {
        SubmitListenerService.fireSuccessfulSubmit(argument);
    }

    private static void replaceGeneratedKeyReferencesWithActualGeneratedKeys(Object[] parameters, List<Object[]> batchIndexGeneratedKeys) {
        if (batchIndexGeneratedKeys != null) {
            for (int i = 0, length = Arrays.length(parameters); i < length; i++) {
                Object value = parameters[i];
                if (value instanceof GeneratedKeyReference ref) {
                    // Getting the indexes (should normally refer to a previous batch already executed at this point)
                    int batchIndex = ref.getStatementBatchIndex();
                    int generatedKeyIndex = ref.getGeneratedKeyIndex();
                    // We get the actual generated key that `ref` was referring to and replace the parameter value with it
                    Object[] generatedKeys = batchIndexGeneratedKeys.get(batchIndex);
                    parameters[i] = generatedKeys[generatedKeyIndex];
                }
            }
        }
    }

    private void log(String message) {
        asyncQueue.log(message);
    }

}
