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
import java.time.LocalDate;

/**
 * @author Bruno Salmon
 */
public abstract class PrimitiveBinaryExpression<T> extends BinaryExpression<T> {

    public PrimitiveBinaryExpression(Expression<T> left, String separator, Expression<T> right, int precedenceLevel) {
        super(left, separator, right, precedenceLevel);
    }

    @Override
    public Object evaluate(Object leftValue, Object rightValue, DomainReader<T> domainReader) {
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
                        if (leftValue instanceof LocalDate)
                            return evaluateLocalDate(Times.toLocalDate(leftValue), Times.toLocalDate(rightValue));
                        return evaluateInstant(Times.toInstant(leftValue), Times.toInstant(rightValue));
                }
            }
        }
        return evaluateObject(leftValue, rightValue);
    }

    protected Object evaluateBoolean(Boolean a, Boolean b) {
        return a == null || b == null ? evaluateObject(a, b) : evaluateBoolean(a.booleanValue(), b.booleanValue());
    }

    protected Object evaluateInteger(Integer a, Integer b) {
        return a == null || b == null ? evaluateObject(a, b) : evaluateInteger(a.intValue(), b.intValue());
    }

    protected Object evaluateLong(Long a, Long b) {
        return a == null || b == null ? evaluateObject(a, b) : evaluateLong(a.longValue(), b.longValue());
    }

    protected Object evaluateFloat(Float a, Float b) {
        return a == null || b == null ? evaluateObject(a, b) : evaluateFloat(a.floatValue(), b.floatValue());
    }

    protected Object evaluateDouble(Float a, Float b) {
        return a == null | b == null ? evaluateObject(a, b) : evaluateDouble(a.doubleValue(), b.doubleValue());
    }

    protected String evaluateString(String a, String b) {
        return Strings.toString(evaluateObject(a, b));
    }

    protected Object evaluateLocalDate(LocalDate a, LocalDate b) {
        return evaluateLong(a.toEpochDay(), b.toEpochDay());
    }

    protected Object evaluateInstant(Instant a, Instant b) {
        return evaluateLong(a.toEpochMilli(), b.toEpochMilli());
    }

    protected abstract boolean evaluateBoolean(boolean a, boolean b);
    protected abstract int evaluateInteger(int a, int b);
    protected abstract long evaluateLong(long a, long b);
    protected abstract float evaluateFloat(float a, float b);
    protected abstract double evaluateDouble(double a, double b);
    protected abstract Object evaluateObject(Object a, Object b);
}
