package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.type.Type;
import dev.webfx.extras.type.Types;
import dev.webfx.platform.util.Booleans;
import dev.webfx.platform.util.time.Times;
import dev.webfx.platform.util.Numbers;
import dev.webfx.platform.util.Strings;

import java.time.Instant;

/**
 * @author Bruno Salmon
 */
public abstract class PrimitiveBinaryBooleanExpression<T> extends BinaryBooleanExpression<T> {

    public PrimitiveBinaryBooleanExpression(Expression left, String separator, Expression right, int precedenceLevel) {
        super(left, separator, right, precedenceLevel);
    }

    @Override
    public Boolean evaluateCondition(Object leftValue, Object rightValue, DomainReader<T> domainReader) {
        Expression<T> left = getLeft();
        Type leftType = left != null ? left.getType() : Types.guessType(leftValue); // left may be null for generic MULTIPLIER and DIVIDER
        PrimType leftPrimType = Types.getPrimType(leftType);
        if (leftPrimType != null) {
            leftValue = domainReader.prepareValueBeforeTypeConversion(leftValue, leftPrimType);
            rightValue = domainReader.prepareValueBeforeTypeConversion(rightValue, leftPrimType);
            if (leftValue != null && rightValue != null) {
                switch (leftPrimType) {
                    case BOOLEAN:
                        return evaluateBoolean(Booleans.toBoolean(leftValue), Booleans.toBoolean(rightValue));
                    case INTEGER:
                        return evaluateInteger(Numbers.toInteger(leftValue), Numbers.toInteger(rightValue));
                    case LONG:
                        return evaluateLong(Numbers.toLong(leftValue), Numbers.toLong(rightValue));
                    case FLOAT:
                        return evaluateFloat(Numbers.toFloat(leftValue), Numbers.toFloat(rightValue));
                    case DOUBLE:
                        return evaluateDouble(Numbers.toDouble(leftValue), Numbers.toDouble(rightValue));
                    case STRING:
                        return evaluateString(Strings.toString(leftValue), Strings.toString(rightValue));
                    case DATE:
                        return evaluateDate(Times.toInstant(leftValue), Times.toInstant(rightValue));
                }
            }
        }
        return evaluateObject(leftValue, rightValue);
    }

    protected Boolean evaluateBoolean(Boolean a, Boolean b) {
        return a == null || b == null ? evaluateObject(a, b) : evaluateBoolean(a.booleanValue(), b.booleanValue());
    }

    protected Boolean evaluateInteger(Integer a, Integer b) {
        return a == null || b == null ? evaluateObject(a, b) : evaluateInteger(a.intValue(), b.intValue());
    }

    protected Boolean evaluateLong(Long a, Long b) {
        return a == null || b == null ? evaluateObject(a, b) : evaluateLong(a.longValue(), b.longValue());
    }

    protected Boolean evaluateFloat(Float a, Float b) {
        return a == null || b == null ? evaluateObject(a, b) : evaluateFloat(a.floatValue(), b.floatValue());
    }

    protected Boolean evaluateDouble(Float a, Float b) {
        return a == null || b == null ? evaluateObject(a, b) : evaluateDouble(a.doubleValue(), b.doubleValue());
    }

    protected Boolean evaluateString(String a, String b) { return evaluateObject(a, b);}

    protected Boolean evaluateDate(Instant a, Instant b) { return evaluateLong(a.toEpochMilli(), b.toEpochMilli());}

    abstract boolean evaluateInteger(int a, int b);
    abstract boolean evaluateLong(long a, long b);
    abstract boolean evaluateFloat(float a, float b);
    abstract boolean evaluateDouble(double a, double b);
    abstract boolean evaluateBoolean(boolean a, boolean b);
    abstract boolean evaluateObject(Object a, Object b);


}
