package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.type.Type;
import dev.webfx.platform.util.Booleans;

/**
 * @author Bruno Salmon
 */
public final class Not<T> extends UnaryExpression<T> {

    public Not(Expression<T> operand) {
        super(operand);
    }

    @Override
    public Type getType() {
        return PrimType.BOOLEAN;
    }

    @Override
    public Object evaluate(T domainObject, DomainReader<T> domainReader) {
        return Booleans.isFalse(super.evaluate(domainObject, domainReader));
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append("not(");
        operand.toString(sb);
        return sb.append(')');
    }
}
