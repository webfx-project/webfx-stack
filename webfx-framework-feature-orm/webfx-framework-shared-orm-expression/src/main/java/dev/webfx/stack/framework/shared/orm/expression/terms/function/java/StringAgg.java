package dev.webfx.stack.framework.shared.orm.expression.terms.function.java;

import dev.webfx.stack.framework.shared.orm.expression.Expression;
import dev.webfx.stack.framework.shared.orm.expression.lci.DomainReader;
import dev.webfx.stack.framework.shared.orm.expression.terms.ExpressionArray;
import dev.webfx.extras.type.PrimType;
import dev.webfx.platform.util.Strings;
import dev.webfx.stack.framework.shared.orm.expression.terms.function.SqlAggregateFunction;

/**
 * @author Bruno Salmon
 */
public final class StringAgg<T> extends SqlAggregateFunction<T> {

    public StringAgg() {
        super("string_agg", null, null, PrimType.STRING, true);
    }

    @Override
    public Object evaluateOnAggregates(T referrer, Object[] aggregates, Expression<T> operand, DomainReader<T> domainReader) {
        String delimiter = ",";
        Expression<T> stringOperand = operand;
        if (operand instanceof ExpressionArray) {
            ExpressionArray<T> array = (ExpressionArray<T>) operand;
            stringOperand = array.getExpressions()[0];
            Expression<T> delimiterOperand = array.getExpressions()[1];
            delimiter = Strings.toSafeString(delimiterOperand.evaluate(referrer, domainReader));
        }
        StringBuilder sb = new StringBuilder();
        for (Object aggregate : aggregates) {
            Object value = stringOperand.evaluate((T) aggregate, domainReader);
            if (value != null) {
                if (sb.length() > 0)
                    sb.append(delimiter);
                sb.append(value);
            }
        }
        return sb.toString();
    }
}
