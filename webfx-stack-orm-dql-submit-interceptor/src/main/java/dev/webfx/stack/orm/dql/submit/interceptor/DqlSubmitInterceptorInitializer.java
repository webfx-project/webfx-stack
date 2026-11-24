package dev.webfx.stack.orm.dql.submit.interceptor;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.db.datascope.KeyDataScope;
import dev.webfx.stack.db.datascope.MultiKeyDataScope;
import dev.webfx.stack.db.datascope.aggregate.AggregateScope;
import dev.webfx.stack.db.datascope.aggregate.AggregateScopeBuilder;
import dev.webfx.stack.db.datascope.schema.SchemaScope;
import dev.webfx.stack.db.datascope.schema.SchemaScopeBuilder;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.submit.ClientSubmitGuard;
import dev.webfx.stack.db.submit.GeneratedKeyReference;
import dev.webfx.stack.db.submit.ProtectedEntityWriteRegistry;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.spi.SubmitServiceProvider;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public class DqlSubmitInterceptorInitializer implements ApplicationJob {

    @Override
    public void onInit() {
        // The client write guard's DQL half: refuses a client write that sets a column the application has declared
        // no client may set. See ClientSubmitGuard and DqlClientSubmitInspector.
        ClientSubmitGuard.registerInspector(new DqlClientSubmitInspector());
        // The purpose of this interceptor is to automatically translate DQL to SQL and compute the schema scope when
        // the submit reaches its local data source (works only with DQL)
        SingleServiceProvider.registerServiceInterceptor(SubmitServiceProvider.class, targetProvider ->
                new SubmitServiceProvider() {
                    @Override
                    public Future<SubmitResult> executeSubmit(SubmitArgument argument) {
                        return interceptAndExecuteSubmit(argument, targetProvider);
                    }
                    @Override
                    public Future<Batch<SubmitResult>> executeSubmitBatch(Batch<SubmitArgument> batch) {
                        return interceptAndExecuteSubmitBatch(batch, targetProvider);
                    }
                });
    }

    /**
     * Reports a submit that will not be translated, and so reaches the database exactly as written.
     *
     * <p>Called here because this is the last moment the distinction exists. Translation replaces the
     * statement and clears the language, so downstream a translated DQL statement and a hand-written SQL
     * one are indistinguishable — which is precisely why a raw statement has been able to travel this
     * far unexamined.
     */
    private static void reportIfNotDql(SubmitArgument argument) {
        String language = argument.getLanguage();
        if (language == null || !"DQL".equalsIgnoreCase(language))
            ProtectedEntityWriteRegistry.notifyNonDqlSubmit(language, argument.getStatement());
    }

    private static Future<SubmitResult> interceptAndExecuteSubmit(SubmitArgument argument, SubmitServiceProvider targetProvider) {
        reportIfNotDql(argument);
        // Bracketed as well as the batch path: a one-statement submit is a caller action like any
        // other, and leaving it ungrouped would make every single-row save look like a submit this
        // inspector never saw the start of.
        Future<ProtectedWrite> authorization;
        ProtectedEntityWriteRegistry.notifySubmitBegin(1);
        try {
            authorization = authorizeAndInspect(argument);
        } finally {
            ProtectedEntityWriteRegistry.notifySubmitEnd();
        }
        return authorization
            .compose(protectedWrite -> targetProvider.executeSubmit(translateSubmit(argument))
                .onSuccess(ignored -> reportIfProtected(protectedWrite)));
    }

    private static Future<Batch<SubmitResult>> interceptAndExecuteSubmitBatch(Batch<SubmitArgument> batch, SubmitServiceProvider targetProvider) {
        // EVERY statement in the batch is authorized, not just the first. A batch is one transaction, so
        // one unauthorized statement anywhere in it is enough to make the whole thing something this
        // caller may not do — and checking only the first would make "hide it behind a legitimate write"
        // the obvious way through.
        for (SubmitArgument argument : batch.getArray())
            reportIfNotDql(argument);
        // Every statement is authorized, and all of them are STARTED HERE, synchronously, before any
        // future completes. Chaining them with compose() ran statements 2..n inside async callbacks,
        // where the caller's identity is no longer readable — so the first statement of a batch was
        // judged as the user and the rest as nobody. The authorizer fails closed, which turned that
        // into refusals rather than a silent hole, but it refused legitimate work either way.
        //
        // That synchronous pass is also what makes the submit boundary worth reporting: the writes of
        // one transaction arrive as a contiguous run, and an inspector told where it starts and stops
        // can answer what ONE caller action contained — which organizations it touched, and whether it
        // could name a single operation. In a finally, so a refusal still closes the group.
        List<Future<ProtectedWrite>> authorizations = new ArrayList<>();
        ProtectedEntityWriteRegistry.notifySubmitBegin(batch.getArray().length);
        try {
            for (SubmitArgument argument : batch.getArray())
                authorizations.add(authorizeAndInspect(argument));
        } finally {
            ProtectedEntityWriteRegistry.notifySubmitEnd();
        }
        return Future.all(new ArrayList<>(authorizations))
            .compose(ignored -> targetProvider.executeSubmitBatch(translateBatch(batch))
                .onSuccess(result -> {
                    for (Future<ProtectedWrite> authorization : authorizations)
                        reportIfProtected(authorization.result());
                }));
    }

    /**
     * The fields a statement SETS, so a policy can distinguish the privileged column from its ordinary
     * neighbours on the same row. Empty for a delete, which sets nothing — a delete is judged on the
     * entity alone, and rightly: removing a row disposes of every field on it.
     *
     * <p>Only plain {@code field = value} assignments are reported. A computed or otherwise unreadable
     * left-hand side contributes no name, which means a policy keyed on field names would not match it —
     * so an entity whose fields are individually protected should also carry an entity-level rule, or a
     * statement this cannot read would slip past on a technicality.
     */
    /**
     * The rows of parameters one argument carries: one normally, several when the client's change set grouped
     * identical statements and replaced their parameter arrays with a {@link Batch} of them
     * ({@code EntityChangesToSubmitBatchGenerator.groupIdenticalStatements}).
     *
     * <p><b>Why this is not a detail.</b> Read as a single write, a grouped argument has no readable value at all:
     * position 0 holds the Batch object and the rest are out of range, so every assignment resolves to null and so
     * does the target. Rules that refuse on an unreadable value would then refuse the whole transaction, and rules
     * that allow on the absence of a value would let every row through on the strength of a value none of them
     * carries. Neither is a reading of what the client actually sent. One write per row is.
     *
     * <p>A row that is not an array of values is kept as null rather than dropped: it is a row this could not
     * read, and the rules must be given the chance to refuse it rather than never being told it was there.
     */
    static java.util.List<Object[]> parameterRowsOf(Object[] parameters) {
        if (parameters != null && parameters.length == 1 && parameters[0] instanceof Batch<?> grouped) {
            Object[] array = grouped.getArray();
            if (array != null && array.length > 0) {
                java.util.List<Object[]> rows = new ArrayList<>(array.length);
                for (Object row : array)
                    rows.add(row instanceof Object[] values ? values : null);
                return rows;
            }
        }
        return java.util.Collections.singletonList(parameters);
    }

    static java.util.Map<String, Object> writtenValuesOf(DqlStatement<Object> dqlStatement, Object[] parameters) {
        ExpressionArray<Object> setClause =
              dqlStatement instanceof Update ? ((Update<Object>) dqlStatement).getSetClause()
            : dqlStatement instanceof Insert ? ((Insert<Object>) dqlStatement).getSetClause()
            : null;
        if (setClause == null)
            return java.util.Collections.emptyMap();
        // Ordered, so the field list handed to a policy reads in statement order rather than hash order.
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        UnnamedPositions unnamed = unnamedPositionsOf(dqlStatement);
        Expression<?>[] assignments = setClause.getExpressions();
        for (int i = 0; i < assignments.length; i++)
            if (assignments[i] instanceof Equals equals && equals.getLeft() instanceof DomainField field) {
                // The FIELD is always recorded, so a field rule still fires. Its value is null only when it
                // IS null; one that could not be read is UNRESOLVED_VALUE, never null (see WriteRequest).
                boolean read;
                Object raw;
                if (unnamed != null && unnamed.setPositions()[i] >= 0) {
                    int position = unnamed.setPositions()[i];
                    read = parameters != null && position < parameters.length;
                    raw = parameterAt(parameters, position);
                } else {
                    read = DqlScopeUtil.isRawValueReadable(equals.getRight(), parameters);
                    raw = DqlScopeUtil.resolveRawValue(equals.getRight(), parameters);
                }
                values.put(field.getName(), read ? writtenValueOf(raw) : ProtectedEntityWriteRegistry.UNRESOLVED_VALUE);
            }
        return values;
    }

    /**
     * One assignment's value, from what the parameter or constant carried: a boolean kept as a Boolean (a row rule may
     * turn on one — whether a person is being made an account owner), a number or string as a string, a
     * {@link GeneratedKeyReference} kept as itself (see below), anything else unknown (null).
     *
     * <p><b>Why the reference is the one non-scalar kept.</b> It carries something no scalar can: this value
     * names a row THIS BATCH is creating, not one that already existed. That is the whole difference between
     * a client attaching a recipient to the mail it just composed and one attaching a recipient to somebody
     * else's mail that is still waiting to be sent — see {@code MailWritePolicy}.
     *
     * <p>It is kept rather than inferred from the null it used to collapse to, because a rule reading that
     * null would be reading "this value could not be determined" as "this was a same-batch reference", and
     * every other unreadable value lands there too. That is the same misreading {@link #targetIdOf} warns
     * about for a null target, and it fails the same way round: open.
     *
     * <p>Strictly narrowing for the rules that already read this map. The one that tests for null,
     * {@code GrantTableWritePolicy}'s "unassigns a role and nothing else", stops treating
     * {@code set role = <a role this batch is inserting>} as an unassignment — which it never was.
     */
    private static Object writtenValueOf(Object raw) {
        if (raw == null || raw instanceof GeneratedKeyReference)
            return raw;
        Object scalar = scalarValueOf(raw);
        // A value that was read but is not a scalar (a date, an array) is not null either: it is something.
        return scalar != null ? scalar : ProtectedEntityWriteRegistry.UNRESOLVED_VALUE;
    }

    /**
     * The scalar reading, used where a value can only sensibly be a scalar: a boolean as a Boolean, a number or
     * string as a string, anything else unknown (null).
     *
     * <p>Kept separate from {@link #writtenValueOf} so that preserving a reference there does not quietly widen
     * {@link ProtectedEntityWriteRegistry.WriteRequest#targetId()}, whose contract is "an id, or null when it
     * could not be determined" and whose readers compare it, print it and bind it as a query parameter. A
     * reference is precisely a value that is NOT an id yet, so it belongs on the null side of that contract.
     */
    private static Object scalarValueOf(Object raw) {
        if (raw instanceof Boolean)
            return raw;
        return raw instanceof Number || raw instanceof String ? String.valueOf(raw) : null;
    }

    private static Object parameterAt(Object[] parameters, int position) {
        return parameters != null && position >= 0 && position < parameters.length ? parameters[position] : null;
    }

    /**
     * Where each unnamed "?" of a write sits in its parameter array: per assignment (-1 where the value is a constant),
     * and for a {@code where id = ?} (-1 otherwise) — or null when the statement is not the simple shape this can vouch
     * for.
     *
     * <p>The legacy JavaFX/GWT change sets write {@code update X set a=?, b=? where id=?}, with the values in that
     * order. A "?" carries no index of its own — every one is the same object — so its position is its order in the
     * compiled SQL, which counts EVERY "?" in the statement, including one inside a subquery. So this reads only the
     * change-set shape itself: every assignment's value a bare "?" or a constant, nothing else. Any other value — an
     * expression, a subquery, a numbered parameter mixed in — answers null, and the values and target are then
     * unknown rather than guessed: guessing is exactly what a subquery holding a "?" would exploit, shifting every
     * later position so a rule reads one row while the write lands on another.
     */
    private record UnnamedPositions(int[] setPositions, int wherePosition) {}

    private static UnnamedPositions unnamedPositionsOf(DqlStatement<Object> dqlStatement) {
        ExpressionArray<Object> setClause =
              dqlStatement instanceof Update ? ((Update<Object>) dqlStatement).getSetClause()
            : dqlStatement instanceof Insert ? ((Insert<Object>) dqlStatement).getSetClause()
            : null;
        Expression<?>[] assignments = setClause == null ? new Expression<?>[0] : setClause.getExpressions();
        int[] positions = new int[assignments.length];
        int next = 0;
        for (int i = 0; i < assignments.length; i++) {
            if (!(assignments[i] instanceof Equals<?> equals))
                return null;
            Expression<?> value = equals.getRight();
            if (value == ParameterReference.UNNAMED_PARAMETER_REFERENCE)
                positions[i] = next++;
            else if (value instanceof Constant)
                positions[i] = -1;
            else
                return null;
        }
        int wherePosition = -1;
        Expression<?> where = dqlStatement.getWhere();
        if (where instanceof Equals<?> equals && equals.getLeft() instanceof DomainField field && "id".equals(field.getName())
            && equals.getRight() == ParameterReference.UNNAMED_PARAMETER_REFERENCE)
            wherePosition = next;
        else if (where != null && !(where instanceof Equals<?> equals2 && equals2.getRight() instanceof Constant))
            return null; // a where this cannot read may hold a "?" of its own
        return new UnnamedPositions(positions, wherePosition);
    }

    /**
     * The id of the row a statement changes, or null when that cannot be read.
     *
     * <p>An answer comes only from an {@code id = value} test, found either as the whole WHERE — what the
     * change set layer generates ({@code update Person set … where id=$2}) — or as one side of a
     * conjunction. Anything else — a disjunction, a subquery, a non-id predicate — returns null. See
     * {@code targetIdIn} for why a conjunction may be entered and a disjunction may not.
     *
     * <p>Returning null for what this cannot read is the whole safety of the thing, and only works
     * because null means UNKNOWN to a policy rather than UNCONSTRAINED. An ownership rule must refuse a
     * write whose target it cannot see: a statement shaped to defeat this extraction is exactly the
     * statement that would be used to reach somebody else's row.
     */
    static Object targetIdOf(DqlStatement<Object> dqlStatement, Object[] parameters) {
        return targetIdIn(dqlStatement.getWhere(), dqlStatement, parameters);
    }

    /**
     * Looks for {@code id = value}, descending conjunctions but never disjunctions.
     *
     * <p><b>An AND is safe to look inside and an OR is not</b>, and the asymmetry is the whole of it:
     * {@code A and B} affects at most the rows {@code A} alone would, so finding {@code id = 7} anywhere
     * in a conjunction proves the statement touches at most row 7. {@code A or B} affects at least those
     * rows and possibly many more, so {@code id = 7 or true} would report 7 while rewriting the table.
     *
     * <p>This exists because the statements that ADD a condition to an id were being read as having no
     * readable target at all — {@code where id=$1 and frontendAccount=$2}, which is the safest shape a
     * client sends, scored the same as a statement with no id in it. A rule refusing unreadable targets
     * would have refused precisely the writes that constrain themselves most.
     */
    private static Object targetIdIn(Expression<?> where, DqlStatement<Object> dqlStatement, Object[] parameters) {
        if (where instanceof And<?> and) {
            Object left = targetIdIn(and.getLeft(), dqlStatement, parameters);
            return left != null ? left : targetIdIn(and.getRight(), dqlStatement, parameters);
        }
        if (where instanceof Equals equals
            && equals.getLeft() instanceof DomainField field
            && "id".equals(field.getName())) {
            if (equals.getRight() == ParameterReference.UNNAMED_PARAMETER_REFERENCE) {
                // The legacy "where id=?": its position is its order — see unnamedPositionsOf
                UnnamedPositions unnamed = unnamedPositionsOf(dqlStatement);
                return unnamed == null || unnamed.wherePosition() < 0 ? null
                    : scalarValueOf(parameterAt(parameters, unnamed.wherePosition()));
            }
            return DqlScopeUtil.resolveScalarValue(equals.getRight(), parameters);
        }
        return null;
    }

    /**
     * Whether this write names no bound at all on which rows it touches.
     *
     * <p><b>The obvious tests are both wrong, and the second one is wrong in a way that looks right.</b>
     * "Has no WHERE" misses {@code where true}. Its replacement — does the WHERE mention any column,
     * which {@code collectPersistentTerms} answers — misses {@code delete from Person where id=id}, and
     * a review caught it after the rule had been written and documented on the strength of the second.
     * {@code id != -1}, {@code id > 0}, {@code id is not null}, {@code $1 = $1} and
     * {@code id in (select …)} all mention columns, all pass that test, and all match every row.
     *
     * <p>So the question is asked the other way round: not "is this unbounded?", which cannot be decided
     * from shape, but "is there a predicate that BINDS?" — an equality or an IN, with a column on one
     * side and a literal or a {@code $n} on the other, reachable through conjunctions only. A shape that
     * does not clearly bind is treated as not binding, which is the direction that refuses rather than
     * the one that allows.
     *
     * <p><b>What it still does not catch, said plainly so nobody reads this as more than it is:</b>
     * {@code where removed = $1} binds by this test and can still match nearly every row. This is a
     * floor against the statement that names no bound whatsoever; it is not an ownership rule, and
     * whose rows a caller may touch is answered by the per-entity authorizer with the caller in hand.
     *
     * <p>An insert is never unbounded: it has no WHERE to be missing, and every insert would otherwise
     * read as the most dangerous shape there is.
     */
    static boolean isUnboundedWrite(ProtectedEntityWriteRegistry.WriteVerb verb, DqlStatement<Object> dqlStatement) {
        return verb != ProtectedEntityWriteRegistry.WriteVerb.INSERT
               && !bindsAnyRows(dqlStatement.getWhere());
    }

    /**
     * Whether any conjunct of this WHERE restricts the rows it matches.
     *
     * <p>Conjunctions only, for the reason {@code targetIdIn} gives: one binding conjunct of
     * {@code A and B} bounds the whole of it, whereas one binding side of {@code A or B} bounds nothing
     * — {@code id = $1 or true} would otherwise read as bound.
     */
    private static boolean bindsAnyRows(Expression<?> where) {
        if (where instanceof And<?> and)
            return bindsAnyRows(and.getLeft()) || bindsAnyRows(and.getRight());
        if (where instanceof Equals<?> equals)
            return bindsColumnToValue(equals.getLeft(), equals.getRight())
                   || bindsColumnToValue(equals.getRight(), equals.getLeft());
        if (where instanceof In<?> in)
            return isColumn(in.getLeft()) && isValueList(in.getRight());
        return false;
    }

    private static boolean bindsColumnToValue(Expression<?> column, Expression<?> value) {
        return isColumn(column) && isValue(value);
    }

    /** A column of the row being written, including one reached through a foreign key. */
    private static boolean isColumn(Expression<?> expression) {
        return expression instanceof DomainField
               || expression instanceof IdExpression
               // `document.person = $1`: a dot whose RIGHT is a column. Deliberately not any dot — a
               // dot may carry an arbitrary expression (`fk.(a or b)`), and `fk.(a or b) = true` binds
               // nothing while looking exactly like an ordinary equality.
               || expression instanceof Dot<?> dot && isColumn(dot.getRight());
    }

    /**
     * A literal or a {@code $n}, judged by SHAPE and not by the value the parameter happens to hold.
     *
     * <p>Reading the value would refuse {@code where list = $1} when $1 arrives null — a statement that
     * matches nothing and did no harm — and a rule that refuses depending on the data is a rule that
     * fails in production and not in a check.
     */
    private static boolean isValue(Expression<?> expression) {
        return expression instanceof Constant || expression instanceof ParameterReference;
    }

    /** {@code in ($1,$2)} or {@code in (1,2)} — never {@code in (select …)}, which bounds nothing here. */
    private static boolean isValueList(Expression<?> expression) {
        if (isValue(expression))
            return true;
        if (!(expression instanceof ExpressionArray<?> array))
            return false;
        for (Expression<?> element : array.getExpressions())
            if (!isValue(element))
                return false;
        return array.getExpressions().length > 0;
    }

    private record ProtectedWrite(String entityName, ProtectedEntityWriteRegistry.WriteVerb verb) {}

    /** Reported only AFTER the write actually landed: an attempt that failed changed nothing to react to. */
    private static void reportIfProtected(ProtectedWrite protectedWrite) {
        if (protectedWrite != null)
            ProtectedEntityWriteRegistry.notifyWriteSucceeded(protectedWrite.entityName(), protectedWrite.verb());
    }

    /**
     * Parses a write once, and feeds the two consumers that want it: the authorizer, when the statement
     * names a protected entity, and the inspector, when the application asked to see this caller's
     * writes.
     *
     * <p>This sits on the DQL path deliberately: here the statement still names an ENTITY and a verb,
     * which is what a policy is written in terms of. By the time it is SQL those have become a table
     * name and a keyword, and recovering the intent from the text would be both harder and easier to
     * fool. The trade is that a statement which never becomes DQL — a raw passthrough — does not pass
     * this point at all; that door is item 7 and is closed separately.
     *
     * <p>Parsing is still the exception rather than the rule: with no inspector interested, a statement
     * is parsed only when the registry's textual pre-filter says a protected name might be involved, so
     * the common write pays one substring scan and one volatile read.
     *
     * <p><b>While an inspector IS interested, every write it wants pays a parse, and that parse is not
     * cached.</b> {@code parseStatement} goes straight to the domain model, unlike
     * {@code parseAndCompileStatement}, which memoises — and {@code translateSubmit} below will take the
     * cached road over the same text a moment later. So an inspected write parses twice. It is accepted
     * rather than optimised because writes are driven by user actions and heartbeats rather than by
     * page loads, which puts this in the tens per second at this system's volumes; if that assumption
     * stops holding, the fix is to hand the inspector the AST this method already has rather than to
     * make it parse again.
     *
     * <p>One parse, not two, and the ordering matters: the inspector is told BEFORE the authorizer is
     * asked, so an inventory records what a caller attempted even when the attempt is about to be
     * refused. What it observes is therefore traffic, not outcomes.
     *
     * <p><b>Inspection never changes a verdict.</b> The only place the two paths meet is an unparseable
     * statement: the inspector is told about it and the protected path still refuses it, because a
     * statement this check could not read must not reach an entity it textually mentions merely because
     * somebody is watching.
     */
    private static Future<ProtectedWrite> authorizeAndInspect(SubmitArgument argument) {
        String statement = argument.getStatement();
        if (argument.getLanguage() == null) // already SQL: not a DQL statement to reason about
            return Future.succeededFuture(null);
        boolean maybeProtected = ProtectedEntityWriteRegistry.mayTouchProtectedEntity(statement);
        // Inspection is DQL-only, where the protected path below deliberately is not. A statement sent
        // as another language (the scheduled-item generator sends "SQL") is not DQL and would fail this
        // parser, so inspecting it would report it as unparseable and overstate how much of a client's
        // traffic this cannot read — the one number an inventory must not get wrong about itself. The
        // protected path keeps its wider guard: a non-DQL statement naming a protected entity is
        // refused for being unreadable, and narrowing that here would be a loosening, not a fix.
        boolean inspecting = "DQL".equalsIgnoreCase(argument.getLanguage())
                             // Asked on THIS thread, before any parse and before any async hop: the
                             // answer depends on request-scoped state the application can still read
                             // here and could not a moment later.
                             && ProtectedEntityWriteRegistry.isInspectingWrite();
        // Told before anything else about this statement: the declaration is the caller's own claim, and
        // reporting it costs nothing whether or not the statement turns out to be parseable, protected,
        // or even a write. Nothing trusts it yet — this step is the measurement that decides whether a
        // caller action can name one operation at all.
        if (inspecting && argument.getWriteIntent() != null)
            ProtectedEntityWriteRegistry.notifyDeclaredIntent(argument.getWriteIntent());
        if (!maybeProtected && !inspecting)
            return Future.succeededFuture(null);
        Object dataSourceId = argument.getDataSourceId();
        if (!LocalDataSourceService.isDataSourceLocal(dataSourceId))
            return Future.succeededFuture(null);
        DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(dataSourceId);
        if (dataSourceModel == null)
            return Future.succeededFuture(null);
        DqlStatement<Object> dqlStatement;
        try {
            dqlStatement = dataSourceModel.parseStatement(statement);
        } catch (RuntimeException e) {
            if (inspecting)
                ProtectedEntityWriteRegistry.notifyUnparseableStatement(statement);
            // Unparseable here but possibly executable later: refuse rather than let something this
            // check could not read reach a protected entity it textually mentions.
            if (maybeProtected)
                return Future.failedFuture("[NotAuthorizedError] Could not parse a statement naming a protected entity");
            return Future.succeededFuture(null);
        }
        ProtectedEntityWriteRegistry.WriteVerb verb =
              dqlStatement instanceof Insert ? ProtectedEntityWriteRegistry.WriteVerb.INSERT
            : dqlStatement instanceof Update ? ProtectedEntityWriteRegistry.WriteVerb.UPDATE
            : dqlStatement instanceof Delete ? ProtectedEntityWriteRegistry.WriteVerb.DELETE
            : null;
        if (verb == null) // not a write
            return Future.succeededFuture(null);
        Object domainClass = dqlStatement.getDomainClass();
        DomainClass resolved = domainClass instanceof DomainClass ? (DomainClass) domainClass
            : dataSourceModel.getDomainModel().getClass(domainClass);
        String entityName = resolved.getName();
        // One write per ROW, for the same reason the client guard reads them that way: a grouped argument read
        // as a single write has no readable value at all, so the authorizer would be judging - and the inventory
        // recording - a reading of nothing, once, for however many rows the client actually sent.
        boolean unbounded = isUnboundedWrite(verb, dqlStatement); // row-invariant: it is a property of the WHERE
        java.util.List<Future<Void>> checks = new ArrayList<>();
        for (Object[] row : parameterRowsOf(argument.getParameters())) {
            java.util.Map<String, Object> writtenValues = writtenValuesOf(dqlStatement, row);
            ProtectedEntityWriteRegistry.WriteRequest request = new ProtectedEntityWriteRegistry.WriteRequest(
                entityName, verb,
                writtenValues.keySet().toArray(String[]::new),
                writtenValues,
                targetIdOf(dqlStatement, row),
                unbounded,
                null); // this path judges one statement at a time, so there is no batch to name
            if (inspecting)
                ProtectedEntityWriteRegistry.notifyWriteInspected(request);
            if (maybeProtected)
                checks.add(ProtectedEntityWriteRegistry.checkWriteAllowed(request));
        }
        if (!maybeProtected)
            return Future.succeededFuture(null);
        return Future.all(new ArrayList<>(checks))
            .map(ignored -> new ProtectedWrite(entityName, verb));
    }

    private static SubmitArgument translateSubmit(SubmitArgument argument) {
        String language = argument.getLanguage();
        Object dataSourceId = argument.getDataSourceId();
        if (language != null && LocalDataSourceService.isDataSourceLocal(dataSourceId)) {
            DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(dataSourceId);
            if (dataSourceModel != null) {
                String statement = argument.getStatement(); // can be DQL or SQL
                String sqlStatement = dataSourceModel.translateStatementIfDql(language, statement);
                if (!statement.equals(sqlStatement)) { // happens when DQL has been translated to SQL
                    //Logger.log("Translated to: " + sqlStatement);
                    argument = SubmitArgument.builder().copy(argument)
                            .setLanguage(null).setStatement(sqlStatement)
                            .addDataScope(createDataScope(statement, dataSourceModel, argument.getParameters()))
                            .build();
                }
            }
        }
        return argument;
    }

    private static Batch<SubmitArgument> translateBatch(Batch<SubmitArgument> batch) {
        return new Batch<>(Arrays.stream(batch.getArray()).map(DqlSubmitInterceptorInitializer::translateSubmit).toArray(SubmitArgument[]::new));
    }

    private static DataScope createDataScope(String dqlSubmitStatement, DataSourceModel dataSourceModel, Object[] parameters) {
        // Returning a wrapper so the scope computation can be skipped if not used later
        // (ex: if intersects method is never called or submit fails)
        return new MultiKeyDataScope() {

            private KeyDataScope[] keyDataScopes;

            @Override
            public KeyDataScope[] getKeyDataScopes() { // Called only if get used
                if (keyDataScopes == null) {
                    DqlStatement<Object> dqlStatement = dataSourceModel.parseStatement(dqlSubmitStatement);
                    DomainClass domainClass = dqlStatement.getDomainClass() instanceof DomainClass ? (DomainClass) dqlStatement.getDomainClass()
                            : dataSourceModel.getDomainModel().getClass(dqlStatement.getDomainClass());
                    Object domainClassId = domainClass.getId();
                    // Building the schema and partition (aggregate) scope. The partition
                    // rules must be SOUND for the rows' state both BEFORE and AFTER the
                    // statement, otherwise queries watching the "old" state miss their
                    // refresh (the historical rooms-drag&drop bug):
                    //  - Update: schema = SET fields; partitions from WHERE equalities on
                    //    fields NOT being modified (their values hold before and after —
                    //    a SET field's OLD value is unknown, so it contributes none).
                    //  - Insert: schema = whole class; partitions from the SET clause
                    //    (the new row's values are the only affected state).
                    //  - Delete: schema = whole class; partitions from WHERE equalities.
                    // Additionally, every written FK WIDENS the schema scope to the
                    // referenced class: PostgreSQL denormalization triggers follow FK
                    // edges (ex: document_line writes update document totals), so the
                    // parent row must be assumed touched — with its identity partition
                    // when the FK value is resolvable, so parent watchers of OTHER rows
                    // can still be skipped.
                    SchemaScopeBuilder ssb = SchemaScope.builder();
                    AggregateScopeBuilder asb = AggregateScope.builder();
                    if (dqlStatement instanceof Update) {
                        Update<Object> update = (Update<Object>) dqlStatement;
                        java.util.Set<Object> setFieldIds = new java.util.HashSet<>();
                        for (Expression<?> expression : update.getSetClause().getExpressions()) {
                            if (expression instanceof Equals && ((Equals<?>) expression).getLeft() instanceof DomainField) {
                                Equals<?> equals = (Equals<?>) expression;
                                DomainField field = (DomainField) equals.getLeft();
                                ssb.addField(field.getDomainClass().getId(), field.getId());
                                setFieldIds.add(field.getId());
                                widenToForeignParent(ssb, asb, field, equals.getRight(), parameters);
                            }
                        }
                        DqlScopeUtil.addPartitions(asb, update.getWhere(), parameters, setFieldIds, domainClassId);
                    } else { // Insert or Delete => all fields of the class are impacted
                        ssb.addClass(domainClassId);
                        if (dqlStatement instanceof Insert) {
                            ExpressionArray<Object> setClause = ((Insert<Object>) dqlStatement).getSetClause();
                            DqlScopeUtil.addPartitions(asb, setClause, parameters, null, domainClassId);
                            for (Expression<?> expression : setClause.getExpressions())
                                if (expression instanceof Equals && ((Equals<?>) expression).getLeft() instanceof DomainField)
                                    widenToForeignParent(ssb, asb, (DomainField) ((Equals<?>) expression).getLeft(), ((Equals<?>) expression).getRight(), parameters);
                        } else if (dqlStatement instanceof Delete) {
                            DqlScopeUtil.addPartitions(asb, dqlStatement.getWhere(), parameters, null, domainClassId);
                            // The deleted rows' FK values are unknown here — widen to every
                            // FK parent class of the model (no identity partitions).
                            for (DomainField field : domainClass.getFields())
                                if (field.getForeignClass() != null)
                                    ssb.addClass(field.getForeignClass().getId());
                        }
                    }
                    SchemaScope schemaScope = ssb.build();
                    AggregateScope aggregateScope = asb.build();
                    // Putting the scopes into the array
                    keyDataScopes = new KeyDataScope[] { schemaScope, aggregateScope };
                }
                return keyDataScopes;
            }
        };
    }

    /**
     * Widens the schema scope to a written FK field's referenced class (trigger
     * cascades follow FK edges), registering the parent row's identity partition
     * when the FK value is a resolvable scalar.
     */
    private static void widenToForeignParent(SchemaScopeBuilder ssb, AggregateScopeBuilder asb, DomainField field, Expression<?> right, Object[] parameters) {
        DomainClass foreignClass = field.getForeignClass();
        if (foreignClass == null)
            return;
        ssb.addClass(foreignClass.getId());
        Object parentId = DqlScopeUtil.resolveScalarValue(right, parameters);
        if (parentId != null)
            asb.addAggregate(DqlScopeUtil.idPartitionType(foreignClass.getId()), parentId);
    }
}
