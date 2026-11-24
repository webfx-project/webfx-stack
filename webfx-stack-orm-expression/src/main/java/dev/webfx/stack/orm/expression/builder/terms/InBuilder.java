package dev.webfx.stack.orm.expression.builder.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.In;
import dev.webfx.stack.orm.expression.terms.function.Call;
import dev.webfx.stack.orm.expression.terms.function.ServerParameterFunction;

/**
 * @author Bruno Salmon
 */
public final class InBuilder extends BinaryBooleanExpressionBuilder {

    /** Whether the right side came from the grammar's bare-term path, which only a server term may take. */
    private final boolean bareRightTerm;

    public InBuilder(ExpressionBuilder left, ExpressionBuilder right) {
        this(left, right, false);
    }

    public InBuilder(ExpressionBuilder left, ExpressionBuilder right, boolean bareRightTerm) {
        super(left, right);
        this.bareRightTerm = bareRightTerm;
    }

    @Override
    protected In newBinaryOperation(Expression left, Expression right) {
        // The grammar lets `in` take a BARE term, with no parentheses and no $, so that a server term
        // supplying many values can be written as `event in CALLER_GRANTED_EVENTS`. The grammar cannot tell
        // one identifier from another, so it accepts any of them and this rejects the rest - otherwise
        // `event in person` would quietly compile to a scalar `in <column>`, where before it was a parse
        // error, and the widening would cost every statement in the system the error it used to get.
        if (bareRightTerm && !(right instanceof Call<?> call
                && call.getFunction() instanceof ServerParameterFunction<?> spf && spf.isArrayValued()))
            throw new IllegalArgumentException("'in' takes a parameter, a list, a subquery, or a server term"
                + " supplying several values - not '" + right + "'");
        return new In(left, right);
    }
}
