package dev.webfx.stack.orm.dql.query.interceptor;

import dev.webfx.stack.db.query.ClientReadDenyList;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.BinaryExpression;
import dev.webfx.stack.orm.expression.terms.DqlStatement;
import dev.webfx.stack.orm.expression.terms.Equals;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import dev.webfx.stack.orm.expression.terms.ParameterReference;
import dev.webfx.stack.orm.expression.terms.Select;
import dev.webfx.stack.orm.expression.terms.SelectExpression;
import dev.webfx.stack.orm.expression.terms.UnaryExpression;
import dev.webfx.stack.orm.expression.terms.Union;
import dev.webfx.stack.orm.expression.terms.WithSelect;

import java.util.List;

/**
 * Decides whether a statement uses a capability column the one way it may: tested for equality against a bound
 * parameter, in a WHERE, and nowhere else.
 *
 * <h3>Why a traversal here, when the denying reader exists precisely to avoid one</h3>
 *
 * <p>Because the two answer different questions. The reader is exhaustive and context-free: it sees every column
 * that reaches SQL and has no idea where in the statement it came from. That is exactly right for a secret, which
 * may not be reached at all. It cannot express "may be compared, may not be selected", because by the time the
 * compiler asks for a column name the difference is gone.
 *
 * <p>So the reader keeps finding these columns — it records them rather than refusing, and the inspector asks here
 * whether the finding was legitimate. This traversal supplies the context the reader cannot, and the reader
 * supplies the trigger this traversal cannot be trusted to find on its own.
 *
 * <h3>Fail-closed is what makes it sound</h3>
 *
 * <p>A traversal is only as complete as its author's memory of the grammar, which is the objection
 * {@code DenyingCompilerDomainModelReader} raises against traversals and it is a fair one. The answer is not to
 * claim completeness but to refuse without it: <b>every node this does not recognise is a refusal</b>, not a pass.
 * A construct nobody thought of therefore costs a refused query rather than a disclosed token.
 *
 * <p>That is affordable here and would not be on the general read path, because only statements that actually
 * reach a capability column are walked — three queries in the whole product today. A false refusal is one broken
 * page, seen immediately, on a path somebody is looking at.
 *
 * @author Claude Code
 */
final class CapabilityColumnWalk {

    private CapabilityColumnWalk() {}

    /** Where in a statement a capability column has been found — only one of these positions is allowed. */
    private enum Position { WHERE, ELSEWHERE }

    /**
     * Null when every occurrence of a capability column is a sanctioned equality test, otherwise why not.
     *
     * <p>The message names no column and no value: it goes to a client.
     *
     * <h3>The reader's count and this walk's must AGREE</h3>
     *
     * <p>This is asked only because the reader has already seen capability columns reach SQL, and it is told how
     * MANY. Every one of them has to be a sanctioned test this walk can point at; anything left over is a read
     * the walk could not see, and the reader is the one to believe, because it sees what the compiler emits
     * while this sees what the statement says.
     *
     * <p>They come apart for real reasons, all three of them live in this model: a fields group expands to its
     * member columns, a field can be defined as an expression, and a bare foreign key pulls in its target's
     * default fields. <b>A statement can therefore read a token without naming it.</b>
     *
     * <p>Requiring merely that the walk found SOMETHING was the first version's defect, and it failed OPEN in the
     * worst way available: one legitimate {@code token = $1} licensed every hidden read beside it, so
     * {@code select firstName,email,<arrival> from VolunteeringApplication where arrivalConfirmationToken=$1 or
     * id>0} returned every live token in the table. The count is what closes that, and it closes the class
     * rather than the three constructs — a fourth would be caught by the same invariant on the day it is added.
     */
    static String refusalFor(DqlStatement<?> parsed, int columnsReachedInSql) {
        return refusalFor(parsed, columnsReachedInSql, false);
    }

    /**
     * @param watched consult the WATCHED set instead of the enforced one — the observation pass, which reports
     *                what this would have refused. The walk and the reader must be looking at the SAME set of
     *                columns or the counts they compare are counts of different things: a legitimate equality
     *                test on a watched column would go unaccounted for, and every test would read as a read.
     */
    static String refusalFor(DqlStatement<?> parsed, int columnsReachedInSql, boolean watched) {
        return outcomeFor(parsed, columnsReachedInSql, watched) == Outcome.CLEAN ? null : refused();
    }

    /**
     * What the walk found, for a caller that needs to tell the two refusals apart.
     *
     * <p>{@link #refusalFor} deliberately cannot: a client learns that its query did not run and nothing about
     * why. The WATCH is the one caller that must, and it is not a client — it reports rather than refuses, and
     * reporting "somebody read the token" when the truth is "this walk met a ternary" produces a count that
     * never reaches zero and therefore an enforced rule that can never be restored. Same reason the enforced
     * side does not care: there, an over-refusal is a visible broken screen that someone fixes within the hour.
     */
    enum Outcome {
        /** Every occurrence was a sanctioned equality test. */
        CLEAN,
        /** The column was READ — named outside a sanctioned test, or reached in SQL without the walk seeing it. */
        READ,
        /** The walk met a construct it does not recognise, so it cannot say. Fail-closed when enforcing. */
        UNANALYSABLE
    }

    static Outcome outcomeFor(DqlStatement<?> parsed, int columnsReachedInSql, boolean watched) {
        Sanctioned sanctioned = new Sanctioned(watched);
        String refusal = statement(parsed, sanctioned);
        if (refusal != null)
            return sanctioned.unanalysable ? Outcome.UNANALYSABLE : Outcome.READ;
        // The counts disagree: the compiler emitted the column more often than the walk could account for, so
        // something read it that the statement does not show — a fields group, an expression-defined field, an
        // inline function body. That is a READ and not an analysis failure.
        return sanctioned.count == columnsReachedInSql ? Outcome.CLEAN : Outcome.READ;
    }

    /**
     * How many occurrences the walk was able to account for — see {@link #refusalFor} — and which set of
     * columns it is accounting for. The mode rides here because this is already threaded through every step of
     * the walk, and because the count and the set it counts must not be able to come apart.
     */
    private static final class Sanctioned {
        private int count;
        /** Set when the walk met something it could not read, which is not the same as finding a read. */
        private boolean unanalysable;
        private final boolean watched;

        Sanctioned(boolean watched) {
            this.watched = watched;
        }
    }

    private static String statement(DqlStatement<?> parsed, Sanctioned sanctioned) {
        if (parsed instanceof Union<?> union) {
            String refusal = select(union.getFirstSelect(), sanctioned);
            if (refusal != null)
                return refusal;
            for (Object[] branch : union.getUnions()) {
                if (branch == null || branch.length < 2 || !(branch[1] instanceof Select<?> branchSelect))
                    return unreadable(sanctioned);
                refusal = select(branchSelect, sanctioned);
                if (refusal != null)
                    return refusal;
            }
            // The union's OWN order by, which belongs to the whole result rather than to any branch and is
            // compiled as such. Walked here because nothing else reaches it: a branch's select() sees only its
            // own clauses.
            return expression(union.getOrderBy(), Position.ELSEWHERE, sanctioned);
        }
        if (parsed instanceof WithSelect<?> with) {
            for (Object[] cte : with.getCtes()) {
                if (cte == null || cte.length < 2)
                    return unreadable(sanctioned);
                // A CTE's body is a select like any other, and its rows reach the result through the main one.
                for (Object element : cte)
                    if (element instanceof Select<?> cteSelect) {
                        String refusal = select(cteSelect, sanctioned);
                        if (refusal != null)
                            return refusal;
                    }
            }
            return select(with.getMainSelect(), sanctioned);
        }
        if (parsed instanceof Select<?> select)
            return select(select, sanctioned);
        return unreadable(sanctioned);
    }

    /**
     * One select: the WHERE may test a capability column, and no other part of it may mention one.
     *
     * <p>Every clause is named explicitly rather than being reached by a generic descent, so that a clause added
     * to {@code Select} in future is one this does not walk — and a statement using it is refused for being
     * unreadable rather than passed for being unexamined.
     */
    private static String select(Select<?> select, Sanctioned sanctioned) {
        if (select == null)
            return unreadable(sanctioned);
        String refusal = expression(select.getWhere(), Position.WHERE, sanctioned);
        if (refusal != null)
            return refusal;
        for (Expression<?> clause : new Expression<?>[] {
                select.getFields(), select.getGroupBy(), select.getHaving(),
                select.getOrderBy(), select.getLimit(), select.getOffset() }) {
            refusal = expression(clause, Position.ELSEWHERE, sanctioned);
            if (refusal != null)
                return refusal;
        }
        // A lateral subquery is a select whose rows join this one; it gets the same treatment rather than being
        // skipped, which is where a column would otherwise hide.
        List<Object[]> laterals = select.getLateralSubqueries();
        if (laterals != null)
            for (Object[] lateral : laterals)
                if (lateral != null)
                    for (Object element : lateral)
                        if (element instanceof Select<?> lateralSelect) {
                            refusal = select(lateralSelect, sanctioned);
                            if (refusal != null)
                                return refusal;
                        }
        return null;
    }

    /**
     * Walks one expression. Unrecognised nodes refuse; a capability column is allowed only as the operand of an
     * equality against a bound parameter, and only inside a WHERE.
     */
    private static String expression(Expression<?> expression, Position position, Sanctioned sanctioned) {
        if (expression == null)
            return null;
        if (expression instanceof Equals<?> equals) {
            // The sanctioned shape, and the only one. A parameter rather than a literal, so that presenting a
            // token is something the caller does through a bind value — which also keeps it out of the statement
            // text that gets logged, cached and compared.
            boolean isSanctioned = position == Position.WHERE
                && (isCapabilityMatch(equals.getLeft(), equals.getRight(), sanctioned)
                    || isCapabilityMatch(equals.getRight(), equals.getLeft(), sanctioned));
            if (isSanctioned) {
                sanctioned.count++;
                return null; // neither side is descended: both have just been accounted for
            }
            String refusal = expression(equals.getLeft(), position, sanctioned);
            return refusal != null ? refusal : expression(equals.getRight(), position, sanctioned);
        }
        if (isCapabilityColumn(expression, sanctioned))
            return refused();
        if (expression instanceof dev.webfx.stack.orm.expression.terms.Symbol<?> symbol) {
            // A symbol that carries an expression is not a leaf: a fields group expands to its member columns
            // and an expression-defined field to whatever it is defined over, so `<arrival>` reads four columns
            // while looking like one name. Descended rather than passed — and even so, the count in
            // refusalFor is what makes the guarantee, because a symbol that expands inside the COMPILER
            // rather than here is invisible from this side.
            Expression<?> defined = symbol.getExpression();
            return defined == null ? null : expression(defined, position, sanctioned);
        }
        if (expression instanceof ParameterReference
            || expression instanceof dev.webfx.stack.orm.expression.terms.Constant
            || expression instanceof dev.webfx.stack.orm.expression.terms.Alias)
            return null; // a leaf, and not a capability column (checked just above)
        if (expression instanceof BinaryExpression<?> binary) {
            String refusal = expression(binary.getLeft(), position, sanctioned);
            return refusal != null ? refusal : expression(binary.getRight(), position, sanctioned);
        }
        if (expression instanceof UnaryExpression<?> unary)
            return expression(unary.getOperand(), position, sanctioned);
        if (expression instanceof ExpressionArray<?> array) {
            Expression<?>[] elements = array.getExpressions();
            if (elements != null)
                for (Expression<?> element : elements) {
                    String refusal = expression(element, position, sanctioned);
                    if (refusal != null)
                        return refusal;
                }
            return null;
        }
        if (expression instanceof SelectExpression<?> subquery) // `in (select …)`, `exists(select …)`
            return select(subquery.getSelect(), sanctioned);
        return unreadable(sanctioned);
    }

    /** {@code token = $1}: this side is a capability column and the other is a bound parameter. */
    private static boolean isCapabilityMatch(Expression<?> column, Expression<?> value, Sanctioned sanctioned) {
        return isCapabilityColumn(column, sanctioned) && value instanceof ParameterReference;
    }

    private static boolean isCapabilityColumn(Expression<?> expression, Sanctioned sanctioned) {
        if (!(expression instanceof DomainField field))
            return false;
        DomainClass domainClass = field.getDomainClass();
        if (domainClass == null)
            return false;
        String table = domainClass.getSqlTableName(), column = field.getSqlColumnName();
        return sanctioned.watched
               ? ClientReadDenyList.isObservedCapabilityColumn(table, column)
               : ClientReadDenyList.isCapabilityColumn(table, column);
    }

    /**
     * Said the same way for both refusals, on purpose.
     *
     * <p>A caller learns that the query did not run and nothing about why — whether the column exists, whether it
     * is protected, or which construct this could not read. Telling the two apart would turn the check into a map
     * of itself.
     */
    private static String refused() {
        return DqlClientQueryInspector.DENIED_REFUSAL;
    }

    /** Said the same way as {@link #refused()} to a client; distinguished only through {@link Outcome}. */
    private static String unreadable(Sanctioned sanctioned) {
        sanctioned.unanalysable = true;
        return DqlClientQueryInspector.DENIED_REFUSAL;
    }
}
