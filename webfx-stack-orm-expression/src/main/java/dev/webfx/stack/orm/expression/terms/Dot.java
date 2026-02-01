package dev.webfx.stack.orm.expression.terms;

import dev.webfx.stack.orm.expression.CollectOptions;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.stack.orm.expression.lci.DomainWriter;
import dev.webfx.stack.orm.expression.terms.function.Call;
import dev.webfx.stack.orm.expression.terms.function.Function;

import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class Dot<T> extends BinaryExpression<T> {

    private final boolean outerJoin;
    private final boolean readLeftKey;

    /* Constructor is private to force the use of the static dot() methods which ensure the left expression is not a Dot */
    private Dot(Expression<T> left, Expression<?> right, boolean outerJoin, boolean readLeftKey) {
        // TODO Avoid this false right cast by extending (new) PipeExpression<T1, T2> instead of BinaryExpression<T>
        super(left, outerJoin ? "?." : ".", (Expression<T>) right, 8);
        this.outerJoin = outerJoin;
        this.readLeftKey = readLeftKey;
    }

    public boolean isOuterJoin() {
        return outerJoin;
    }

    public boolean isReadLeftKey() {
        return readLeftKey;
    }

    @Override
    public Expression<?> getForwardingTypeExpression() {
        return right;
    }

    @Override
    public Object evaluate(T domainObject, DomainReader<T> domainReader) {
        Object leftValue = left.evaluate(domainObject, domainReader);
        if (leftValue == null)
            return null;
        T rightData = domainReader.getDomainObjectFromId(leftValue, domainObject);
        return right.evaluate(rightData, domainReader);
    }

    @Override
    public Object evaluate(Object leftValue, Object rightValue, DomainReader<T> domainReader) {
        return null; // never called due to the above evaluate method override
    }

    @Override
    public void setValue(T domainObject, Object value, DomainWriter<T> dataWriter) {
        Object leftValue = left.evaluate(domainObject, dataWriter);
        if (leftValue != null) {
            T rightData = dataWriter.getDomainObjectFromId(leftValue, domainObject);
            right.setValue(rightData, value, dataWriter);
        }
    }

    @Override
    public void collect(CollectOptions options) {
        left.collect(options);
        if (!options.factorizeLeftDot())
            right.collect(options);
        else {
            CollectOptions rightOptions = CollectOptions.sameButEmpty(options);
            right.collect(rightOptions);
            List<Expression<T>> rightTerms = rightOptions.getCollectedTerms();
            if (!rightTerms.isEmpty()) {
                Dot<T> persistentDot;
                if (rightTerms.size() != 1) {
                    persistentDot = Dot.dot(left, new ExpressionArray<>(rightTerms), outerJoin);
                }
                else if (rightTerms.get(0) == right) {
                    persistentDot = this;
                }
                else {
                    persistentDot = Dot.dot(left, rightTerms.get(0), outerJoin);
                }
                Expression<T> expandLeft = persistentDot.expandLeft();
                if (expandLeft == persistentDot) {
                    options.addTerm(persistentDot);
                }
                else {
                    expandLeft.collect(options);
                }
            }
        }
    }

    public Expression<T> expandLeft() {
        if (left instanceof Call) {
            Call<T> call = (Call<T>) this.left;
            Function<T> function = call.getFunction();
            if (function.isIdentity())
                return new Call<>(function.getName(), Dot.dot(call.getOperand(), getRight(), isOuterJoin()).expandLeft(), call.getOrderBy());
        }
        if (left instanceof Dot) {
            return dot(left, getRight(), isOuterJoin(), true).expandLeft();
        }
        Expression<?> leftForwardingTypeExpression = left.getForwardingTypeExpression();
        if (leftForwardingTypeExpression == left)
            return this;
        if (leftForwardingTypeExpression instanceof Dot) {
            return dot((Dot<T>) leftForwardingTypeExpression, getRight(), isOuterJoin()).expandLeft();
        }
        if (leftForwardingTypeExpression instanceof TernaryExpression) {
            TernaryExpression<T> leftTernaryExpression = (TernaryExpression<T>) leftForwardingTypeExpression;
            return new TernaryExpression<>(leftTernaryExpression.getQuestion(), dot(leftTernaryExpression.getYes(), getRight(), isOuterJoin()).expandLeft(), dot(leftTernaryExpression.getNo(), getRight(), isOuterJoin()).expandLeft());
        }
        return this;
    }

    public static <T> Dot<T> dot(Expression<T> left, Expression<?> right) {
        return dot(left, right, false);
    }

    public static <T> Dot<T> dot(Expression<T> left, Expression<?> right, boolean outerJoin) {
        return dot(left, right, outerJoin, true);
    }

    public static <T> Dot<T> dot(Expression<T> left, Expression<?> right, boolean outerJoin, boolean readLeftKey) {
        if (left instanceof Dot leftDot) {
            return new Dot<>(leftDot.getLeft(), dot(leftDot.getRight(), right, outerJoin, readLeftKey), leftDot.isOuterJoin(), readLeftKey);
        }
        return new Dot<>(left, right, outerJoin, readLeftKey);
    }
}
