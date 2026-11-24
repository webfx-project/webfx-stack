package dev.webfx.stack.db.query.cli;

import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryService;

/**
 * Runs a DQL statement as this caller, through the same service call a browser app makes.
 *
 * <h3>Counts, and why they are not capped</h3>
 *
 * <p>A count is {@code select count(1) as n from ...} — ordinary DQL, answered by the server, and so never
 * bounded by how many rows the tool was willing to carry. That is what lets every invariant be a comparison
 * of counts: {@code scoped(E) <= unscoped(E)} says nothing if either side might have been truncated on the
 * way out.
 *
 * <h3>Rows, and why the limit is the server's</h3>
 *
 * <p>{@code limit} and {@code offset} are in the DQL grammar, so a row probe is paged by appending them to
 * the statement and letting the server do the work. The alternative — fetching everything and printing the
 * first hundred — would move the caller's rows into this process before deciding not to look at them, which
 * is the opposite of what an instrument about read scope should do.
 *
 * <p>Appending is refused when the statement already ends in a {@code limit} or {@code offset} of its own,
 * because two limits is a statement nobody wrote and the quieter outcome — silently honouring one of them —
 * would make a paged result look complete.
 *
 * @author Claude Code
 */
public final class DqlRunner {

    /** The language every client statement must declare; a null language is refused by the server as raw SQL. */
    public static final String DQL = "DQL";

    private final Object dataSourceId;
    private final long timeoutMillis;

    public DqlRunner(Object dataSourceId, long timeoutMillis) {
        this.dataSourceId = dataSourceId;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Builds the argument a probe is sent as.
     *
     * <p>{@code hasDqlRuntime} is false because this tool has no DQL runtime: it reads a result's values, it
     * does not evaluate expression terminals. That is the same thing the React clients say, and it tells the
     * server to compile expression fields to SQL rather than sending terminals back.
     *
     * <p>No parameter NAMES are ever set. A client that names a parameter is refused outright if the name
     * begins {@code caller.}, and this tool asks for caller values the only legitimate way: by writing the
     * term in the statement and letting the endpoint attach the value. {@link ClientPath#refusalFor} is the
     * floor under that.
     */
    public QueryArgument argumentFor(String statement, Object[] parameters) {
        return QueryArgument.builder()
            .setDataSourceId(dataSourceId)
            .setLanguage(DQL)
            .setStatement(statement)
            .setParameters(parameters)
            .setHasDqlRuntime(false)
            // The builder defaults this to FALSE, which tells the server the client has the column names
            // cached - and this tool has no cache, so it would print 'col0' for every column it was asked to
            // show. A probe prints column names or it prints nothing useful.
            .setSendMetadata(true)
            .build();
    }

    /** Runs a statement as written. */
    public DqlResult run(String statement, Object[] parameters) {
        QueryArgument argument = argumentFor(statement, parameters);
        String refusal = ClientPath.refusalFor(argument);
        if (refusal != null)
            throw new IllegalArgumentException(refusal);
        return new DqlResult(ClientRuntime.await(QueryService.executeQuery(argument), timeoutMillis,
                                                 "The query"));
    }

    /** Runs a statement with the server paging it. */
    public DqlResult runPaged(String statement, Object[] parameters, int limit, int offset) {
        return run(paged(statement, limit, offset), parameters);
    }

    /**
     * {@code statement} with {@code limit} and {@code offset} appended.
     *
     * <p>Separate from {@link #runPaged} and public so that what the server will be asked is testable without
     * a server — the paging is the part a reader has to be able to trust, because a wrong one does not look
     * wrong in the output.
     */
    public static String paged(String statement, int limit, int offset) {
        if (statement == null)
            throw new IllegalArgumentException("There is no statement to page.");
        if (limit <= 0)
            throw new IllegalArgumentException("A row limit must be positive; " + limit + " was given.");
        if (offset < 0)
            throw new IllegalArgumentException("A row offset cannot be negative; " + offset + " was given.");
        String alreadyPaged = existingPagingClause(statement);
        if (alreadyPaged != null)
            throw new IllegalArgumentException(
                "This statement already ends with its own '" + alreadyPaged + "' clause, so the tool will not"
                + " add another: two of them is a statement nobody wrote, and honouring one of them silently"
                + " would make a paged result look complete. Remove it, or run it with -c as written.");
        String paged = statement.strip();
        if (paged.endsWith(";"))
            paged = paged.substring(0, paged.length() - 1).strip();
        return paged + " limit " + limit + (offset > 0 ? " offset " + offset : "");
    }

    /**
     * The paging keyword this statement already carries at its own top level, or null when it carries none.
     *
     * <p>Looks only at what follows the LAST closing parenthesis, which is what separates the statement's own
     * trailing clause from a subquery's. {@code ... (select id from X order by id limit 5) and agent} carries
     * no top-level paging — the limit belongs to the subquery and does not collide with an appended one —
     * while {@code ... where y in (select id from X) limit 5} does. A statement with no parentheses at all is
     * scanned whole.
     *
     * <p>Deliberately a text test rather than a parse: this module has no DQL compiler, and the consequence of
     * each error is asymmetric. Over-refusing costs the operator one flag and a clear message; under-refusing
     * sends a statement with two {@code limit} clauses, and whichever the server honours, the result looks
     * complete and is not.
     */
    static String existingPagingClause(String statement) {
        String tail = statement.strip().toLowerCase();
        if (tail.endsWith(";"))
            tail = tail.substring(0, tail.length() - 1).strip();
        int lastClose = tail.lastIndexOf(')');
        if (lastClose >= 0)
            tail = tail.substring(lastClose + 1);
        for (String token : tail.split("[\\s(),]+")) {
            if ("limit".equals(token))
                return "limit";
            if ("offset".equals(token))
                return "offset";
        }
        return null;
    }
}
