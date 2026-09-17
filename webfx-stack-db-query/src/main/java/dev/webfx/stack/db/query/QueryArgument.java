package dev.webfx.stack.db.query;

import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.db.datascope.DataScope;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class QueryArgument {

    public static final int STANDARD_PRIORITY = 0;

    /**
     * Priority delta the push server applies to <strong>re-fires</strong> of an existing subscription
     * (i.e. when an already-pushed query re-executes because a relevant submit invalidated its result).
     * The <em>initial</em> fire keeps the subscription's chosen priority — first paint competes fairly
     * with one-shot queries — but subsequent refreshes drop a tier so they yield to user-facing work
     * (one-shot reads, submits) when the server's AsyncQueue is contended. A push refresh's freshness
     * is user-visible (so it shouldn't sink all the way to {@code BACKGROUND}) but it's also not
     * latency-critical (so it shouldn't compete with the initial fetch a user is staring at).
     */
    public static final int PUSH_REFRESH_PRIORITY_DELTA = -5;

    private final transient QueryArgument originalArgument;
    private final Object dataSourceId;
    private final DataScope dataScope;
    private final String language;
    private final String statement;
    private final Object[] parameters;
    private final String[] parameterNames; // null if parameters are not named, otherwise contains each parameter name in the same order as the `parameters` array
    private final boolean sendMetadata; // when false, the server omits columnNames from the QueryResult (client has them cached)
    private final boolean hasDqlRuntime; // when false, the server compiles DQL expression fields to SQL instead of sending terminals for client-side evaluation
    private final int priority; // execution priority for the server-side AsyncQueue; higher value = sooner; STANDARD_PRIORITY (0) is the default
    private final int callId; // caller identity (one per ReactiveQueryCall instance); the server combines it with the client's runId to derive a stable coalescing key. 0 means unset (no coalescing).
    private final int callSeq; // per-fire sequence number; the server echoes it in the QueryResult so the caller can filter out stale results. 0 means unset.
    private final boolean shedWhenBusy; // when true, the server rejects this query fast (instead of queueing) if its executor is at capacity — for optional revalidations whose caller holds a cached fallback

    public QueryArgument(QueryArgument originalArgument, Object dataSourceId, DataScope dataScope, String language, String statement, Object[] parameters, String[] parameterNames) {
        this(originalArgument, dataSourceId, dataScope, language, statement, parameters, parameterNames, false, true, STANDARD_PRIORITY, 0, 0, false);
    }

    public QueryArgument(QueryArgument originalArgument, Object dataSourceId, DataScope dataScope, String language, String statement, Object[] parameters, String[] parameterNames, boolean sendMetadata) {
        this(originalArgument, dataSourceId, dataScope, language, statement, parameters, parameterNames, sendMetadata, true, STANDARD_PRIORITY, 0, 0, false);
    }

    public QueryArgument(QueryArgument originalArgument, Object dataSourceId, DataScope dataScope, String language, String statement, Object[] parameters, String[] parameterNames, boolean sendMetadata, boolean hasDqlRuntime) {
        this(originalArgument, dataSourceId, dataScope, language, statement, parameters, parameterNames, sendMetadata, hasDqlRuntime, STANDARD_PRIORITY, 0, 0, false);
    }

    public QueryArgument(QueryArgument originalArgument, Object dataSourceId, DataScope dataScope, String language, String statement, Object[] parameters, String[] parameterNames, boolean sendMetadata, boolean hasDqlRuntime, int priority, int callId, int callSeq) {
        this(originalArgument, dataSourceId, dataScope, language, statement, parameters, parameterNames, sendMetadata, hasDqlRuntime, priority, callId, callSeq, false);
    }

    public QueryArgument(QueryArgument originalArgument, Object dataSourceId, DataScope dataScope, String language, String statement, Object[] parameters, String[] parameterNames, boolean sendMetadata, boolean hasDqlRuntime, int priority, int callId, int callSeq, boolean shedWhenBusy) {
        this.originalArgument = originalArgument;
        this.dataSourceId = dataSourceId;
        this.dataScope = dataScope;
        this.language = language;
        this.statement = statement;
        this.parameters = parameters;
        this.parameterNames = parameterNames;
        this.sendMetadata = sendMetadata;
        this.hasDqlRuntime = hasDqlRuntime;
        this.priority = priority;
        this.callId = callId;
        this.callSeq = callSeq;
        this.shedWhenBusy = shedWhenBusy;
    }

    public QueryArgument getOriginalArgument() {
        return originalArgument;
    }

    public Object getDataSourceId() {
        return dataSourceId;
    }

    public DataScope getDataScope() {
        return dataScope;
    }

    public String getLanguage() {
        return language;
    }

    public String getStatement() {
        return statement;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    /** When false, the server should omit columnNames from the QueryResult (the client has them cached). */
    public boolean isSendMetadata() {
        return sendMetadata;
    }

    /** When false, the server compiles DQL expression fields to SQL instead of sending terminals for client-side evaluation. */
    public boolean isHasDqlRuntime() {
        return hasDqlRuntime;
    }

    /** Execution priority used by the server's query queue. Higher = sooner. Default is {@link #STANDARD_PRIORITY}. */
    public int getPriority() {
        return priority;
    }

    /**
     * Caller identity, stable per source (typically per {@code ReactiveQueryCall} instance). The server combines this
     * with the client's runId to derive the AsyncQueue coalescing key — so a new query from the same caller supersedes
     * any earlier pending one. Returns 0 when unset; servers should treat 0 as "no coalescing".
     */
    public int getCallId() {
        return callId;
    }

    /**
     * Per-fire sequence number. The server echoes it verbatim into the {@link QueryResult} so the caller can detect
     * stale results (i.e. a result that arrives after a newer fire has already returned). Returns 0 when unset.
     */
    public int getCallSeq() {
        return callSeq;
    }

    /**
     * When true, the server rejects this query fast (a "ServerBusy"-prefixed error) instead of queueing it when its
     * executor is at capacity. For optional background revalidations whose caller holds a cached fallback — under
     * load, shedding them at the door protects mandatory queries instead of letting optional ones pile up.
     * Default is false (the query queues normally).
     */
    public boolean isShedWhenBusy() {
        return shedWhenBusy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryArgument that = (QueryArgument) o;

        if (!Objects.equals(dataSourceId, that.dataSourceId)) return false;
        if (!Objects.equals(language, that.language)) return false;
        if (!Objects.equals(statement, that.statement)) return false;

        // We need to be quite flexible with the parameters, especially with the numbers, as they might be of different
        // types after (de)serialisation (ex: a 5 Integer can become a 5 Short, but they should still be considered as
        // equals). So we will use Numbers.sameValue() for the check.

        // Note: it's ok to consider parameter Object[0] and null to be equals
        int n1 = parameters == null ? 0 : parameters.length;
        int n2 = that.parameters == null ? 0 : that.parameters.length;
        if (n1 != n2) return false;
        for (int i = 0; i < n1; i++) {
            Object p1 = parameters[i], p2 = that.parameters[i];
            if (!Numbers.identicalObjectsOrNumberValues(p1, p2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = dataSourceId.hashCode();
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (statement != null ? statement.hashCode() : 0);
        result = 31 * result + java.util.Arrays.hashCode(parameters);
        return result;
    }

    /**
     * Describes a bind-parameter array by its size, never its contents.
     *
     * <p>Deliberately returns no sample and no types: a "first value" or a type list is still the
     * caller's data, and the point of a count is that it can be logged anywhere without thought.
     * Diagnosis keeps what it actually needs - the statement, the parameter names, and how many
     * values were supplied.
     */
    public static String describeParameters(Object[] parameters) {
        if (parameters == null)
            return "null";
        return parameters.length + (parameters.length == 1 ? " value" : " values");
    }

    @Override
    public String toString() {
        return "QueryArgument{" +
               "dataSourceId=" + dataSourceId +
               ", language='" + language + '\'' +
               ", statement='" + statement + '\'' +
               // Bind values are the application's data - for a booking query, names, email
               // addresses and free-text health notes. They are NOT rendered here: this toString()
               // is interpolated into log lines and error messages all over the stack, so printing
               // them here puts personal data wherever any of those end up. Count only. Parameter
               // NAMES are metadata, not values, so they stay.
               ", parameters=" + describeParameters(parameters) +
               ", parameterNames=" + Arrays.toString(parameterNames) +
               '}';
    }

    public static QueryArgumentBuilder builder() {
        return new QueryArgumentBuilder();
    }

}
