package dev.webfx.stack.orm.dql.query.interceptor;

import dev.webfx.stack.db.query.ClientReadInspectionRegistry.ReadShape;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.dql.sqlcompiler.ExpressionSqlCompiler;
import dev.webfx.stack.orm.dql.sqlcompiler.lci.CompilerDomainModelReader;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.dbms.DbmsSqlSyntax;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Alias;
import dev.webfx.stack.orm.expression.terms.And;
import dev.webfx.stack.orm.expression.terms.Constant;
import dev.webfx.stack.orm.expression.terms.Dot;
import dev.webfx.stack.orm.expression.terms.DqlStatement;
import dev.webfx.stack.orm.expression.terms.Equals;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import dev.webfx.stack.orm.expression.terms.IdExpression;
import dev.webfx.stack.orm.expression.terms.In;
import dev.webfx.stack.orm.expression.terms.Or;
import dev.webfx.stack.orm.expression.terms.ParameterReference;
import dev.webfx.stack.orm.expression.terms.Select;
import dev.webfx.stack.orm.expression.terms.Symbol;
import dev.webfx.stack.orm.expression.terms.BinaryExpression;
import dev.webfx.stack.orm.expression.terms.SelectExpression;
import dev.webfx.stack.orm.expression.terms.UnaryExpression;
import dev.webfx.stack.orm.expression.terms.TernaryExpression;
import dev.webfx.stack.orm.expression.terms.Union;
import dev.webfx.stack.orm.expression.terms.WithSelect;
import dev.webfx.stack.orm.expression.terms.function.Call;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Describes one client read: what it is rooted at, what it reaches, and what its WHERE ties down.
 *
 * <p>Step 0 of the read-authorization plan. It answers questions of FACT and none of judgement — whether the
 * facts add up to a query scoped to its caller is the application's to decide, because only the application
 * knows what an ownership predicate is called in this domain.
 *
 * <h3>Why this never decides anything</h3>
 *
 * <p>It is driven from {@link DqlClientQueryInspector} on a pass of its own, after the verdict and separately
 * from it, and it swallows everything it cannot read. A description that could fail a query would make turning
 * observation on a change of behaviour, which is the one thing step 0 must not be.
 *
 * <h3>What "bound" means here: intersection of rows versus union of rows</h3>
 *
 * <p>Everything below reports what a statement GUARANTEES about the rows it can return, never what it merely
 * mentions. One rule generates the rest, and it is the only one worth remembering: <b>a conjunction narrows the
 * rows, an alternative widens them</b>, so
 *
 * <ul>
 *   <li><b>AND is satisfied by ANY side.</b> {@code A and B} matches at most the rows {@code A} alone would, so
 *       one binding conjunct binds the whole of it.</li>
 *   <li><b>OR and UNION need EVERY side.</b> {@code A or B} matches the rows of both, so it guarantees something
 *       only if both sides do. {@code id = $1 or true} guarantees nothing; {@code id = $1 or parent = $1}
 *       guarantees plenty, and reporting THAT as unconstrained is the mistake this paragraph exists to prevent.
 *       A union is the same operation spelled differently, and is treated identically.</li>
 * </ul>
 *
 * <p><b>Bounded is a property of the statement; a bound FIELD is a property shared by every alternative.</b>
 * The two come apart, which is why they are reported separately. The front office expands
 * {@code accountCanAccessPersonOrders($1, person)} into a union by hand, because the macro's OR spans two person
 * rows and defeats every index: one branch binds {@code person.frontendAccount}, the other
 * {@code person.accountPerson.frontendAccount}. No single field is bound in both, and the statement is
 * nonetheless scoped to one account. Reporting only the intersection called the orders page unconstrained — and
 * an enforcement rule written from that would have refused the codebase's own documented performance pattern.
 *
 * <p>A guard is a function in CONJUNCT position and must hold on EVERY alternative, for the same reason: an
 * ownership predicate in one branch of an OR leaves the other branch returning whatever it likes. A guard is
 * reported but never treated as constraining the rows, because whether a name means "scoped" cannot be known
 * from here — {@code searchMatchesPerson(p)} sits in the same position as an ownership predicate and scans the
 * table. The application is asked instead, which is the division this module draws everywhere else.
 *
 * <p>What is deliberately NOT read as binding: {@code x in (select …)}. A subquery can select anything, so it
 * bounds nothing this can verify. That leaves such a statement described as unconstrained when in practice it is
 * not — the safe direction, and the one case where the conservative answer is kept on purpose.
 *
 * @author Claude Code
 */
final class DqlClientReadShaper {

    private DqlClientReadShaper() {}

    /**
     * What one expression, branch or statement guarantees about the rows it returns.
     *
     * @param fields      bound in EVERY alternative — what the statement guarantees whichever branch a row came
     *                    through
     * @param anyFields   bound in at least one alternative. Only meaningful alongside {@code binds}: with it,
     *                    these are the fields the statement is scoped by, spread across its branches
     * @param functions   guards holding on EVERY alternative
     * @param binds       whether a FIELD is tied to a value — the question {@code fields} cannot answer on its
     *                    own, because two branches can each bind and share no field. Deliberately not set by a
     *                    function guard: see {@code factsOf}
     */
    private record Facts(Set<String> fields, Set<String> anyFields, Set<String> functions, boolean binds) {

        static Facts none() {
            return new Facts(new TreeSet<>(), new TreeSet<>(), new TreeSet<>(), false);
        }

        /** AND: the rows are the intersection, so either side binding binds the whole. */
        Facts and(Facts other) {
            return new Facts(union(fields, other.fields), alternatives(this, other),
                             union(functions, other.functions), binds || other.binds);
        }

        /** OR and UNION: the rows are the union, so a guarantee survives only if BOTH sides make it. */
        Facts or(Facts other) {
            return new Facts(intersection(fields, other.fields), alternatives(this, other),
                             intersection(functions, other.functions), binds && other.binds);
        }

        /**
         * The fields that actually tie rows down, taken only from sides that DO.
         *
         * <p>A side that binds nothing contributes no alternatives, and the guard is the point rather than an
         * optimisation. The front office's past-orders query carries
         * {@code (event.vodExpirationDate==null or event.vodExpirationDate < now())} — an OR whose second side
         * is a comparison, so the whole of it binds nothing. Merging its fields regardless put
         * {@code event.vodExpirationDate} into the {@code any-of} list, which an operator is told is "what the
         * statement is scoped by". It is a VOD window, it scopes nothing, and crediting a branch with a scope
         * it does not have is the permissive direction — the one this whole model exists to avoid.
         */
        private static Set<String> alternatives(Facts left, Facts right) {
            Set<String> r = new TreeSet<>();
            if (left.binds)
                r.addAll(left.anyFields);
            if (right.binds)
                r.addAll(right.anyFields);
            return r;
        }

        private static Set<String> union(Set<String> a, Set<String> b) {
            Set<String> r = new TreeSet<>(a);
            r.addAll(b);
            return r;
        }

        private static Set<String> intersection(Set<String> a, Set<String> b) {
            Set<String> r = new TreeSet<>(a);
            r.retainAll(b);
            return r;
        }
    }

    /**
     * The shape of this statement, or null when it could not be described.
     *
     * <p>Null is not a verdict. A statement this cannot read is one the guard decides about on its own terms —
     * usually by refusing it — and the caller reports it as undescribable so that the inventory says how much of
     * the traffic it is failing to cover rather than quietly overstating itself.
     */
    static ReadShape shapeOf(DataSourceModel dataSourceModel, String statement, boolean compileExpressions) {
        DqlStatement<?> parsed;
        try {
            parsed = dataSourceModel.parseStatement(statement);
        } catch (RuntimeException e) {
            return null;
        }
        String kind = parsed instanceof Union ? "union" : parsed instanceof WithSelect ? "with"
            : parsed instanceof Select ? "select" : null;
        if (kind == null) // an insert, update or delete sent to the query endpoint: not a read to describe
            return null;
        Set<String> tables = new TreeSet<>();
        Set<String> constructs = new TreeSet<>();
        try {
            compileCollecting(dataSourceModel, parsed, compileExpressions, tables);
        } catch (RuntimeException e) {
            // It parsed and would not compile. The tables collected up to that point are a partial answer, and a
            // partial answer presented as a whole one is worse than none: report it as undescribable instead.
            return null;
        }
        try {
            collectConstructs(parsed, constructs);
        } catch (RuntimeException e) {
            // Observing what a statement CONTAINS must not cost the record of it. A field body parsed lazily
            // here can throw where the guard's own compile never touched it, and letting that escape lost the
            // read entirely — recorded as neither a shape nor undescribable, and re-walked on every repeat
            // because nothing was cached.
            constructs.clear();
            constructs.add("(constructs unreadable)");
        }
        List<Select<?>> branches = branchesOf(parsed);
        Facts guaranteed = acrossBranches(branches);
        boolean hasWhere = !branches.isEmpty();
        for (Select<?> branch : branches)
            hasWhere &= branch != null && branch.getWhere() != null;
        return new ReadShape(
            entityNameOf(dataSourceModel, branches.isEmpty() ? null : branches.get(0)),
            kind,
            tables.toArray(new String[0]),
            guaranteed.fields().toArray(new String[0]),
            guaranteed.anyFields().toArray(new String[0]),
            guaranteed.functions().toArray(new String[0]),
            constructs.toArray(new String[0]),
            guaranteed.binds(),
            hasWhere);
    }

    /**
     * Every select whose rows can reach the result.
     *
     * <p>A CTE's body is deliberately NOT one of them: its rows reach the result only through the main select,
     * which is where the constraint that matters is applied. Its tables are collected by the compilation
     * regardless, which is the part a description must not miss.
     */
    private static List<Select<?>> branchesOf(DqlStatement<?> parsed) {
        List<Select<?>> branches = new ArrayList<>();
        if (parsed instanceof Union<?> union) {
            branches.add(union.getFirstSelect());
            for (Object[] branch : union.getUnions())
                if (branch != null && branch.length > 1 && branch[1] instanceof Select<?> select)
                    branches.add(select);
        } else if (parsed instanceof WithSelect<?> with) {
            branches.add(with.getMainSelect());
        } else if (parsed instanceof Select<?> select) {
            branches.add(select);
        }
        return branches;
    }

    /**
     * What every branch guarantees.
     *
     * <p>A union is an OR spelled across statements, so it combines exactly as one: one unconstrained branch
     * leaves the whole statement guaranteeing nothing, and a field counts as bound only where every branch
     * binds it.
     */
    private static Facts acrossBranches(List<Select<?>> branches) {
        Facts combined = null;
        for (Select<?> branch : branches) {
            Facts branchFacts = branch == null ? Facts.none()
                : factsOf(branch.getWhere(), "", branch.getDomainClassAlias());
            combined = combined == null ? branchFacts : combined.or(branchFacts);
        }
        return combined == null ? Facts.none() : combined;
    }

    /**
     * Every expression kind the statement uses, across every clause of every branch.
     *
     * <p>Deliberately NOT the guarantees walk: that one descends conjunctions only, because it is establishing
     * what a statement promises. This one descends everything, because it is establishing what a statement
     * CONTAINS — the question a restricted dialect has to be defined against.
     *
     * <p>An unrecognised node has its class name recorded and its children left unvisited. Recording it is the
     * whole point: the list of constructs nobody anticipated is the output.
     */
    private static void collectConstructs(DqlStatement<?> parsed, Set<String> into) {
        if (parsed == null)
            return;
        into.add(parsed.getClass().getSimpleName());
        if (parsed instanceof Union<?> union) {
            collectConstructs(union.getFirstSelect(), into);
            for (Object[] branch : union.getUnions())
                if (branch != null && branch.length > 1 && branch[1] instanceof Select<?> s)
                    collectConstructs(s, into);
            collectExpressionConstructs(union.getOrderBy(), into);
            return;
        }
        if (parsed instanceof WithSelect<?> with) {
            for (Object[] cte : with.getCtes())
                if (cte != null)
                    for (Object element : cte)
                        if (element instanceof Select<?> s)
                            collectConstructs(s, into);
            collectConstructs(with.getMainSelect(), into);
            return;
        }
        if (!(parsed instanceof Select<?> select))
            return;
        List<Object[]> from = select.getAdditionalFromEntities(), laterals = select.getLateralSubqueries();
        if (from != null && !from.isEmpty())
            into.add("+additionalFrom");
        if (laterals != null && !laterals.isEmpty()) {
            into.add("+lateral");
            for (Object[] lateral : laterals)
                if (lateral != null)
                    for (Object element : lateral)
                        if (element instanceof Select<?> s)
                            collectConstructs(s, into);
        }
        for (Expression<?> clause : new Expression<?>[] {
                select.getFields(), select.getWhere(), select.getGroupBy(), select.getHaving(),
                select.getOrderBy(), select.getLimit(), select.getOffset() })
            collectExpressionConstructs(clause, into);
    }

    private static void collectExpressionConstructs(Expression<?> expression, Set<String> into) {
        if (expression == null)
            return;
        into.add(expression.getClass().getSimpleName());
        if (expression instanceof SelectExpression<?> subquery) {
            collectConstructs(subquery.getSelect(), into);
            return;
        }
        if (expression instanceof Call<?> call) {
            collectExpressionConstructs(call.getOperand(), into);
            collectExpressionConstructs(call.getOrderBy(), into); // `f(x order by …)`, which the grammar allows
            return;
        }
        if (expression instanceof Symbol<?> symbol) {
            collectExpressionConstructs(symbol.getExpression(), into);
            return;
        }
        if (expression instanceof BinaryExpression<?> binary) {
            collectExpressionConstructs(binary.getLeft(), into);
            collectExpressionConstructs(binary.getRight(), into);
            return;
        }
        if (expression instanceof UnaryExpression<?> unary) {
            collectExpressionConstructs(unary.getOperand(), into);
            return;
        }
        if (expression instanceof ExpressionArray<?> array) {
            Expression<?>[] elements = array.getExpressions();
            if (elements != null)
                for (Expression<?> element : elements)
                    collectExpressionConstructs(element, into);
            return;
        }
        // A ternary is not a Unary or a Binary, so its three operands are reached only by naming it — and a
        // subquery hiding in one of its branches is exactly what this walk exists to surface.
        if (expression instanceof TernaryExpression<?> ternary) {
            collectExpressionConstructs(ternary.getQuestion(), into);
            collectExpressionConstructs(ternary.getYes(), into);
            collectExpressionConstructs(ternary.getNo(), into);
            return;
        }
        // anything else: its own name is recorded above, and its children are not walked
    }

    /** The compilation the guard performs, with the collecting reader in place of the denying one. */
    private static void compileCollecting(DataSourceModel dataSourceModel, DqlStatement<?> parsed,
                                          boolean compileExpressions, Set<String> tables) {
        CompilerDomainModelReader reader =
            new CollectingCompilerDomainModelReader(dataSourceModel.getCompilerDomainModelReader(), tables);
        DbmsSqlSyntax syntax = dataSourceModel.getDbmsSqlSyntax();
        if (parsed instanceof WithSelect<?> withSelect)
            ExpressionSqlCompiler.compileWithSelect(withSelect, syntax, true, true, compileExpressions, reader);
        else if (parsed instanceof Union<?> union)
            ExpressionSqlCompiler.compileUnion(union, syntax, true, true, compileExpressions, reader);
        else if (parsed instanceof Select<?> select)
            ExpressionSqlCompiler.compileSelect(select, syntax, true, true, compileExpressions, reader);
    }

    private static String entityNameOf(DataSourceModel dataSourceModel, Select<?> select) {
        if (select == null)
            return null;
        try {
            Object domainClass = select.getDomainClass();
            if (domainClass instanceof DomainClass resolved)
                return resolved.getName();
            DomainClass resolved = dataSourceModel.getDomainModel().getClass(domainClass);
            return resolved == null ? null : resolved.getName();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * What one expression guarantees: the field paths it ties to a value, and the functions it uses as guards.
     *
     * <p>The prefix carries the dot path accumulated on the way in, so {@code document.(event = $1)} is recorded
     * as {@code document.event} rather than as a bare {@code event} that could belong to any table.
     *
     * <p>Paths and never values: {@code cart.uuid}, not the uuid. The uuid is a capability and the address in
     * {@code lower(email) = $1} is somebody's; neither belongs in an inventory, and the column name answers the
     * question an inventory is asking anyway.
     */
    private static Facts factsOf(Expression<?> where, String prefix, String rootAlias) {
        if (where == null)
            return Facts.none();
        if (where instanceof And<?> and)
            return factsOf(and.getLeft(), prefix, rootAlias).and(factsOf(and.getRight(), prefix, rootAlias));
        if (where instanceof Or<?> or)
            return factsOf(or.getLeft(), prefix, rootAlias).or(factsOf(or.getRight(), prefix, rootAlias));
        if (where instanceof Dot<?> dot) {
            // `fk.(a and b)` — the left is the path, the right is a condition on the target. Any other dot is a
            // column reference, which the equality cases below read.
            String left = pathOf(dot.getLeft(), rootAlias);
            return left == null ? Facts.none()
                : factsOf(dot.getRight(), left.isEmpty() ? prefix : prefix + left + ".", rootAlias);
        }
        if (where instanceof Call<?> call) {
            // A function in CONJUNCT position is a boolean guard on the rows — which is where an ownership
            // predicate lives. Its arguments are deliberately not descended: a guard inside an argument guards
            // nothing.
            //
            // It is reported, and it does NOT set `binds`. The two are different claims and only one of them
            // can be made from here: that a function restricts the rows is unknowable to this module, which
            // sees a name. `searchMatchesPerson(p)` is a live client search condition and scans the table when
            // the search term is empty; counting it as binding filed a table-wide scan as constrained, which is
            // the direction an inventory must never err in. Whether a given name means "scoped" is the
            // application's to say, and `functions` is how it is asked.
            String name = call.getFunctionName();
            if (name == null || name.isEmpty())
                return Facts.none();
            Set<String> functions = new TreeSet<>();
            functions.add(name);
            return new Facts(new TreeSet<>(), new TreeSet<>(), functions, false);
        }
        if (where instanceof Equals<?> equals) {
            String path = bindingPath(equals.getLeft(), equals.getRight(), prefix, rootAlias);
            if (path == null)
                path = bindingPath(equals.getRight(), equals.getLeft(), prefix, rootAlias);
            return path == null ? Facts.none() : boundTo(path);
        }
        if (where instanceof In<?> in && isValueList(in.getRight())) {
            String path = pathOf(in.getLeft(), rootAlias);
            return path == null || path.isEmpty() ? Facts.none() : boundTo(prefix + path);
        }
        return Facts.none();
    }

    private static Facts boundTo(String path) {
        Set<String> fields = new TreeSet<>();
        fields.add(path);
        return new Facts(fields, new TreeSet<>(fields), new TreeSet<>(), true);
    }

    private static String bindingPath(Expression<?> column, Expression<?> value, String prefix, String rootAlias) {
        if (!isValue(value))
            return null;
        String path = pathOf(column, rootAlias);
        return path == null || path.isEmpty() ? null : prefix + path;
    }

    /**
     * The dotted name of a column, or null for anything that is not one.
     *
     * <p>Deliberately not any dot: a dot may carry an arbitrary expression ({@code fk.(a or b)}), and
     * {@code fk.(a or b) = true} binds nothing while looking exactly like an ordinary equality.
     *
     * <p>A single-argument function around a column IS read through, so {@code lower(email) = $1} is recorded
     * as binding {@code email}. That is the shape the front office's email lookup sends, and recording it as
     * binding nothing would have hidden the one read in the inventory that turns an address into a name.
     * {@code coalesce(a, b)} is not read through — its argument is a list, not a column.
     *
     * <p><b>The statement's own alias resolves to the empty path, not to null</b>, which is what makes
     * {@code select … from DocumentLine dl where dl.document.person = $1} describable at all. An alias is a local
     * name for the root the statement already names, so {@code dl.document.person} and {@code document.person}
     * are the same column and must produce the same path, or an inventory would file the same read under two
     * shapes depending on whether its author happened to declare an alias — and, worse, report the aliased one
     * as guaranteeing nothing. Any OTHER alias keeps its name as a step, because it is not this root.
     */
    private static String pathOf(Expression<?> expression, String rootAlias) {
        if (expression instanceof IdExpression)
            return "id";
        if (expression instanceof Alias<?> alias) {
            String name = alias.getName();
            if (name == null || name.isEmpty())
                return null;
            return name.equals(rootAlias) ? "" : name;
        }
        if (expression instanceof Call<?> call)
            return pathOf(call.getOperand(), rootAlias);
        if (expression instanceof Symbol<?> symbol) {
            String name = symbol.getName();
            return name == null || name.isEmpty() ? null : name;
        }
        if (expression instanceof Dot<?> dot) {
            String left = pathOf(dot.getLeft(), rootAlias), right = pathOf(dot.getRight(), rootAlias);
            if (left == null || right == null)
                return null;
            return left.isEmpty() ? right : right.isEmpty() ? left : left + "." + right;
        }
        return null;
    }

    /** A literal or a {@code $n}, judged by SHAPE and never by the value a parameter happens to hold. */
    private static boolean isValue(Expression<?> expression) {
        return expression instanceof Constant || expression instanceof ParameterReference;
    }

    /** {@code in ($1,$2)} or {@code in (1,2)} — never {@code in (select …)}, which guarantees nothing here. */
    private static boolean isValueList(Expression<?> expression) {
        if (isValue(expression))
            return true;
        if (!(expression instanceof ExpressionArray<?> array))
            return false;
        Expression<?>[] elements = array.getExpressions();
        if (elements == null || elements.length == 0)
            return false;
        for (Expression<?> element : elements)
            if (!isValue(element))
                return false;
        return true;
    }
}
