package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.*;

/**
 * @author Bruno Salmon
 */
public final class BooleanExpressionSqlCompiler extends BinaryExpressionSqlCompiler<BinaryBooleanExpression<?>> {

    public BooleanExpressionSqlCompiler() {
        super(And.class, Equals.class, GreaterThan.class, GreaterThanOrEquals.class,
                In.class, LessThan.class, LessThanOrEquals.class, Like.class, NotEquals.class, NotLike.class, Or.class,
                All.class, Any.class);
    }

    @Override
    public void compileExpressionToSql(BinaryBooleanExpression<?> e, Options o) {
        Expression<?> left = e.getLeft();
        Expression<?> right = e.getRight();
        boolean isLeftNull = left == Constant.NULL;
        boolean isRightNull = right == Constant.NULL;
        if (isLeftNull || isRightNull) {
            String specialOperator = e instanceof Equals ? " is null" : e instanceof NotEquals ? " is not null" : null;
            if (specialOperator != null) {
                compileChildExpressionToSql(isRightNull ? left : right, o);
                o.build.prepareAppend(o.clause, specialOperator);
                return;
            }
        }
        boolean lowerLeftPrecedence = getSqlPrecedenceLevel(left) < e.getPrecedenceLevel();
        StringBuilder sb = o.build.prepareAppend(o);
        if (lowerLeftPrecedence)
            sb.append('(');
        o = o.changeSeparator(null);
        compileChildExpressionToSql(left, o);
        if (lowerLeftPrecedence)
            sb.append(')');
        // `x in $1` (bare parameter, as opposed to the `x in ($1, $2, ...)` scalar list) means membership of the
        // ARRAY parameter $1, and compiles to `x = any($1)` — Postgres can't bind an array to a scalar `in ($1)`
        boolean inArrayParameter = e instanceof In && right instanceof ParameterReference;
        o.build.prepareAppend(o.clause, inArrayParameter ? "=any(" : getSqlSeparator(e, o));
        boolean lowerRightPrecedence = !inArrayParameter && getSqlPrecedenceLevel(right) < e.getPrecedenceLevel();
        if (lowerRightPrecedence)
            sb.append('(');
        if (e instanceof In) {
            o = o.changeReadForeignFields(false); /* stop reading foreign fields with in (select ...) since only 1 field must be present */
            if (right instanceof ExpressionArray)
                o = o.changeSeparator(", ");
        }
        compileChildExpressionToSql(right, o);
        if (lowerRightPrecedence || inArrayParameter)
            sb.append(')');
    }

    private int getSqlPrecedenceLevel(Expression<?> e) {
        int precedenceLevel = e.getPrecedenceLevel();
        if (e instanceof Dot<?> dot) // may be decomposed (ex: document.(cancelled or !arrived) -> document.cancelled or document.!arrived)
            precedenceLevel = Math.min(precedenceLevel, getSqlPrecedenceLevel(dot.getRight()));
        return precedenceLevel;
    }
}
