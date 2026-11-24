package dev.webfx.stack.orm.dql.query.interceptor;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.query.ClientQueryGuard;
import dev.webfx.stack.db.query.ClientReadDenyList;
import dev.webfx.stack.db.query.ClientReadInspectionRegistry;
import dev.webfx.stack.db.query.ClientReadInspectionRegistry.ReadShape;
import dev.webfx.stack.db.query.ProtectedEntityReadRegistry;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.dql.sqlcompiler.ExpressionSqlCompiler;
import dev.webfx.stack.orm.dql.sqlcompiler.lci.CompilerDomainModelReader;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.dbms.DbmsSqlSyntax;
import dev.webfx.stack.orm.expression.terms.DqlStatement;
import dev.webfx.stack.orm.expression.terms.Select;
import dev.webfx.stack.orm.expression.terms.Union;
import dev.webfx.stack.orm.expression.terms.WithSelect;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Enforces {@link ClientReadDenyList} on DQL: compiles each client query a second time, through a reader that
 * refuses the denied tables and columns, and lets it run only if that compilation succeeds.
 *
 * <h3>Why compile again, rather than reuse the real compilation</h3>
 *
 * <p>Because the real one is cached by statement text. The server's own code reads the secret columns — signing
 * somebody in means reading their sign-in code — and a client sending the identical text would be handed that
 * cached compilation without the reader ever being consulted. So this compiles on its own fresh parse, never
 * touches that cache, and keeps a verdict cache of its own that only it can fill.
 *
 * <h3>Mirrors the real compilation exactly, on purpose</h3>
 *
 * <p>Same statement kinds, same flags — generating the query mapping and reading default foreign fields, as the
 * interceptor does. That matters for one case in particular: selecting a bare foreign key loads the target's
 * default fields, so a looser compilation here could approve a query whose real compilation then reaches a column
 * this one never saw.
 *
 * @author Claude Code
 */
final class DqlClientQueryInspector implements ClientQueryGuard.Inspector {

    static final String DENIED_REFUSAL = "This query reads data a client may not";

    /** Statements already found clean, keyed with the deny-list version they were checked against. */
    private final Map<String, Boolean> allowed = new HashMap<>();
    private static final int MAX_ALLOWED = 5_000;

    /**
     * Descriptions already derived, keyed by statement text — the deny list does not enter into a description,
     * so unlike {@link #allowed} this is not versioned against it.
     */
    private final Map<String, ReadShape> shapes = new HashMap<>();
    private static final int MAX_SHAPES = 5_000;

    /**
     * Cached in place of a description for a statement that has none, so that a statement this cannot read is
     * not recompiled on every occurrence. Compared by identity and never passed to anyone.
     */
    private static final ReadShape UNDESCRIBABLE =
        new ReadShape(null, null, new String[0], new String[0], new String[0], new String[0], new String[0], false, false);

    @Override
    public String refusalReason(QueryArgument argument) {
        // Step 0 of the read-authorization plan, and a pass of its own: the description below cannot reach the
        // verdict, cannot fail a query, and runs BEFORE the decision so that an inventory records what a caller
        // attempted even when the attempt is about to be refused. What it observes is traffic, not outcomes.
        // Asked on THIS thread, while the caller's state is still on it.
        boolean inspecting = ClientReadInspectionRegistry.isInspectingRead();
        if (!"DQL".equalsIgnoreCase(argument.getLanguage())) {
            // Reported before the refusal rather than after it, because the number of statements an inventory
            // CANNOT describe is what says whether it covers all of a client's traffic or most of it — and a
            // client still sending raw SQL is exactly the traffic that answer is about. Counted even though the
            // guard is about to refuse it: what is being measured is what was attempted.
            if (inspecting)
                ClientReadInspectionRegistry.notifyUndescribableStatement(argument.getStatement());
            // The interceptor treats any language at all as DQL to compile; a client naming another one has no
            // purpose this check could anticipate, so it is refused rather than guessed at.
            return ClientQueryGuard.RAW_SQL_REFUSED;
        }
        if (inspecting)
            describe(argument);
        String refusal = decide(argument);
        if (refusal != null)
            return refusal;
        return enforceReadPolicy(argument);
    }

    /**
     * Asks the application's read policy, when one is registered — step 3 of the read-authorization plan.
     *
     * <p>Deliberately AFTER {@link #decide}: the deny list is about columns nobody may read and answers
     * without a caller, so it stays the cheaper and blunter gate in front.
     *
     * <p><b>Independent of whether anyone is watching.</b> The shape is computed through the same cached
     * {@link #shapeFor} the description uses, rather than inside {@link #describe}, so that enforcing a policy
     * never depends on an inspector being registered. Tying the two together would have made a control come
     * and go with an observer.
     *
     * <p><b>Fails closed, and only once something is declared.</b> A statement that cannot be shaped cannot be
     * judged, so with a policy registered it is refused rather than run — the same rule
     * {@code ClientSubmitGuard} states for writes. With no policy registered nothing is declared, there is
     * nothing to enforce, and an unshapeable statement runs as it always has.
     */
    private String enforceReadPolicy(QueryArgument argument) {
        if (!ProtectedEntityReadRegistry.hasPolicy())
            return null;
        ReadShape shape;
        try {
            shape = shapeFor(argument);
        } catch (Throwable shapingFailed) {
            // Unlike the description, this one must NOT be swallowed: a shaping failure here is a statement
            // nobody checked, and a policy has been declared.
            return ClientQueryGuard.UNCHECKABLE_REFUSED;
        }
        if (shape == null || shape == UNDESCRIBABLE)
            return ClientQueryGuard.UNCHECKABLE_REFUSED;
        return ProtectedEntityReadRegistry.refusalReason(new ProtectedEntityReadRegistry.ReadRequest(
            shape.entityName(), shape.touchedTables(), shape.boundFields(), shape.bounded(), shape.hasWhere()));
    }

    /**
     * This statement's shape, computed once and cached by text, for both the description and the enforcement.
     *
     * <p>Returns {@link #UNDESCRIBABLE} when there is nothing to shape — no statement, no local data source —
     * and otherwise whatever the shaper made of it. It throws what the shaper throws: the description swallows
     * that and carries on, the enforcement refuses. Keeping the two reactions at the call sites is the point
     * of lifting this out of {@code describe}.
     */
    private ReadShape shapeFor(QueryArgument argument) {
        String statement = argument.getStatement();
        Object dataSourceId = argument.getDataSourceId();
        if (statement == null || !LocalDataSourceService.isInitialised()
            || !LocalDataSourceService.isDataSourceLocal(dataSourceId))
            return UNDESCRIBABLE;
        boolean compileExpressions = !argument.isHasDqlRuntime();
        String key = compileExpressions + "|" + statement;
        ReadShape shape;
        synchronized (shapes) {
            shape = shapes.get(key);
        }
        if (shape == null) {
            shape = DqlClientReadShaper.shapeOf(
                DataSourceModelService.getDataSourceModel(dataSourceId), statement, compileExpressions);
            if (shape == null)
                shape = UNDESCRIBABLE;
            synchronized (shapes) {
                if (shapes.size() >= MAX_SHAPES)
                    shapes.clear(); // crude, but bounded; a statement costs one extra compile to re-earn
                shapes.put(key, shape);
            }
        }
        return shape;
    }

    /**
     * Describes one client read for whoever is watching, and swallows everything it cannot.
     *
     * <p>Separate from {@link #decide} rather than folded into it, at the cost of one extra compilation the
     * first time a statement is seen. Sharing the decision's compilation would have meant the description
     * depending on where a denial happened to stop, and — worse — the two becoming one thing that could fail a
     * query for a reason to do with watching it. Descriptions are cached by statement text, so the steady state
     * is a map lookup.
     */
    private void describe(QueryArgument argument) {
        try {
            ReadShape shape = shapeFor(argument);
            if (shape == UNDESCRIBABLE)
                ClientReadInspectionRegistry.notifyUndescribableStatement(argument.getStatement());
            else
                ClientReadInspectionRegistry.notifyRead(shape);
        } catch (Throwable ignored) {
            // Watching a query must never be able to fail one, and a broken description is not worth a refusal.
        }
        try {
            // Its OWN try, not the description's. Sharing one meant that anything thrown while SHAPING a
            // statement skipped the watch for it — silently, permanently, and with nothing cached to retry. A
            // statement in that state contributes an invisible zero to the count that decides whether the
            // enforced rule can come back, which is the one number this must not quietly get wrong.
            watchCapabilityColumns(DataSourceModelService.getDataSourceModel(argument.getDataSourceId()),
                                   argument.getStatement(), !argument.isHasDqlRuntime());
        } catch (Throwable ignored) {
        }
    }

    /**
     * Reports a client reading a WATCHED capability column — the rule of
     * {@link ClientReadDenyList#observeColumnExceptEqualityMatch}, applied without the refusal.
     *
     * <p>Its own compilation, through a reader that denies nothing and counts only the watched set. Sharing
     * the enforcing pass would have been cheaper and wrong twice over: that pass stops at its first denial, so
     * it would report whatever happened to come before one, and its counter is weighed against the walk's
     * sanctioned count for the ENFORCED columns — a comparison that means nothing once two different sets are
     * added into it. The counting rule has already had one bug of exactly that shape.
     *
     * <p><b>Not cached, deliberately.</b> The shape cache above keys a description; this needs to fire on every
     * OCCURRENCE, because the number being collected is how often clients still read the column, not how many
     * distinct ways they phrase it. Measured cost of the extra parse and compile: 11 µs for a trivial select,
     * 24 µs for a typical booking read, 33 µs for the orders union — against a millisecond-scale round trip to
     * the database. When nothing is watched it is one volatile read.
     */
    private void watchCapabilityColumns(DataSourceModel dataSourceModel, String statement, boolean compileExpressions) {
        if (ClientReadDenyList.hasNoObservedColumns() || dataSourceModel == null)
            return;
        DenyingCompilerDomainModelReader watcher =
            new DenyingCompilerDomainModelReader(dataSourceModel.getCompilerDomainModelReader(), true);
        DqlStatement<?> parsed;
        try {
            parsed = compile(dataSourceModel, statement, compileExpressions, watcher);
        } catch (RuntimeException e) {
            return; // does not parse or compile: the enforcing pass refuses it, and there is nothing to describe
        }
        // Flat watches first, and WITHOUT the walk: every appearance of such a column is a read, so there is
        // nothing to reconcile and no construct that could excuse one. Reported even when the capability tally
        // below is empty, which is the usual case — the two watches answer different questions.
        if (!watcher.flatWatchedColumnsReached().isEmpty())
            ClientReadInspectionRegistry.notifyObservedColumnRead(
                statement, watcher.flatWatchedColumnsReached().toArray(new String[0]));
        int hits = watcher.capabilityColumnHits();
        if (hits == 0)
            return;
        // Same rule as the enforced one: every occurrence must be a sanctioned equality test the walk can point
        // at. Anything left over is a READ — but "the walk does not recognise this construct" is NOT a read, and
        // conflating them would keep the count above zero for a statement that only ever tested the column.
        CapabilityColumnWalk.Outcome outcome = CapabilityColumnWalk.outcomeFor(parsed, hits, true);
        if (outcome != CapabilityColumnWalk.Outcome.CLEAN)
            ClientReadInspectionRegistry.notifyObservedCapabilityColumnRead(
                statement, watcher.watchedColumnsReached().toArray(new String[0]),
                outcome == CapabilityColumnWalk.Outcome.UNANALYSABLE);
    }

    private String decide(QueryArgument argument) {
        if (ClientReadDenyList.isEmpty())
            return null; // nothing declared secret: nothing to find, and nothing worth compiling twice for
        Object dataSourceId = argument.getDataSourceId();
        if (!LocalDataSourceService.isInitialised() || !LocalDataSourceService.isDataSourceLocal(dataSourceId))
            // Before the data source is up — or for one this server does not own — there is no domain model to
            // compile against, so nothing can be vouched for.
            return ClientQueryGuard.UNCHECKABLE_REFUSED;
        String statement = argument.getStatement();
        boolean compileExpressions = !argument.isHasDqlRuntime();
        String key = ClientReadDenyList.version() + "|" + compileExpressions + "|" + statement;
        synchronized (allowed) {
            if (allowed.containsKey(key))
                return null;
        }
        DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(dataSourceId);
        try {
            compileForClient(dataSourceModel, statement, compileExpressions);
        } catch (ClientReadDeniedException denied) {
            // Named here and nowhere else: the operator needs to know what was reached for, the client does not.
            Console.log("🛡 Client query reached " + denied.getMessage() + " — refused");
            return DENIED_REFUSAL;
        } catch (RuntimeException e) {
            // Not a denial: the statement does not parse or compile. The real compilation would fail the same way,
            // but refusing here rather than letting it through keeps this check the only thing between a client and
            // the database — and the message is the one the real path would have returned anyway.
            return e.getMessage() != null ? e.getMessage() : ClientQueryGuard.UNCHECKABLE_REFUSED;
        }
        synchronized (allowed) {
            if (allowed.size() >= MAX_ALLOWED)
                allowed.clear(); // crude, but bounded; a clean statement costs one extra compile to re-earn
            allowed.put(key, Boolean.TRUE);
        }
        return null;
    }

    /**
     * The server-supplied names this statement binds once compiled under {@code scopeToken}.
     *
     * <p>Answered from the COMPILED form, which is the only thing that knows. A data scope condition is
     * injected by the compiler and appears nowhere in the client's text, so the text test
     * {@code ServerCallerParameters} uses for an unscoped caller cannot see the terms it carries — a statement
     * reading a scoped entity needs {@code CALLER_PERSON} while saying nothing of the kind. Compiling says so
     * exactly: the parameter names of the result are the names it will bind.
     *
     * <p>Precise in both directions on purpose, and the second one is the expensive one to get wrong.
     * Attaching a caller value makes the argument caller-specific, and a push subscription is keyed by its
     * argument — so answering "all of them" for every statement would give every viewer of a shared query a
     * stream of their own. A statement that reads nothing scoped must come back empty-handed so those
     * arguments stay equal and those streams stay shared.
     *
     * <p>Uses the cached compile the interceptor is about to perform anyway: {@code parseAndCompileSelect}
     * memoises by statement and token, so this costs the compile once per shape, not once per query.
     *
     * <p>Null on anything it cannot compile — no data source yet, an unparseable statement, a shape this does
     * not handle. The caller reads null as "could not say" and attaches everything, which is the direction
     * that keeps a query running.
     */
    @Override
    public String[] serverCallerNamesNeeded(QueryArgument argument, Object scopeToken) {
        Object dataSourceId = argument == null ? null : argument.getDataSourceId();
        if (dataSourceId == null || scopeToken == null || !LocalDataSourceService.isInitialised()
                || !LocalDataSourceService.isDataSourceLocal(dataSourceId))
            return null;
        try {
            DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(dataSourceId);
            if (dataSourceModel == null)
                return null;
            List<String> names = dataSourceModel
                .parseAndCompileSelect(argument.getStatement(), !argument.isHasDqlRuntime(), scopeToken)
                .getParameterNames();
            if (names == null)
                return new String[0];
            List<String> callerNames = new ArrayList<>();
            for (String name : names)
                if (name != null && name.startsWith("caller.") && !callerNames.contains(name))
                    callerNames.add(name);
            return callerNames.toArray(new String[0]);
        } catch (Throwable e) {
            return null; // could not say; the caller attaches everything rather than nothing
        }
    }

    /** The dispatch DataSourceModel.compileSelectOrWithSelect performs, with the denying reader in place. */
    // Package-private so the check can drive the exact compilation the inspector uses, against a real domain model.
    static void compileForClient(DataSourceModel dataSourceModel, String statement, boolean compileExpressions) {
        DenyingCompilerDomainModelReader reader = new DenyingCompilerDomainModelReader(dataSourceModel.getCompilerDomainModelReader());
        DqlStatement<?> parsed = compile(dataSourceModel, statement, compileExpressions, reader);
        // The reader has found a column that may be TESTED but not read. Whether the finding was legitimate is a
        // question about where in the statement it happened, which the reader cannot see and the walk can.
        int capabilityHits = reader.capabilityColumnHits();
        if (capabilityHits > 0 && CapabilityColumnWalk.refusalFor(parsed, capabilityHits) != null)
            throw new ClientReadDeniedException("a capability column", null);
    }

    /**
     * One compilation through the given reader, returning what was parsed.
     *
     * <p>Shared by the enforcing pass and the watching one so that the two cannot drift: what is being watched
     * has to be the same compilation that would be refused, or the watch says nothing about the refusal. Only
     * the READER differs between them.
     */
    // Package-private for the same reason as compileForClient: so the check drives the exact compilation the
    // inspector does, rather than a reimplementation of it that can drift from it.
    static DqlStatement<?> compile(DataSourceModel dataSourceModel, String statement,
                                   boolean compileExpressions, CompilerDomainModelReader reader) {
        DbmsSqlSyntax syntax = dataSourceModel.getDbmsSqlSyntax();
        DqlStatement<?> parsed = dataSourceModel.parseStatement(statement);
        if (parsed instanceof WithSelect<?> withSelect)
            ExpressionSqlCompiler.compileWithSelect(withSelect, syntax, true, true, compileExpressions, reader);
        else if (parsed instanceof Union<?> union)
            ExpressionSqlCompiler.compileUnion(union, syntax, true, true, compileExpressions, reader);
        else if (parsed instanceof Select<?> select)
            ExpressionSqlCompiler.compileSelect(select, syntax, true, true, compileExpressions, reader);
        else
            // An insert, update or delete sent to the QUERY endpoint. The real path would fail casting it; this
            // refuses it on purpose instead of relying on that.
            throw new IllegalArgumentException("Only a select may be sent as a query");
        return parsed;
    }
}
