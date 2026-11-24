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

    /**
     * {@inheritDoc}
     *
     * <p><b>When factorizing, the left is NOT collected as a term of its own.</b> It used to be, and for a
     * foreign key that made every dotted path collect two terms rather than one: {@code document.ref} gave
     * back {@code document} AND {@code document.ref}. The first of those is a BARE foreign key, and a bare
     * foreign key in a select clause means something specific to the SQL compiler — "load that entity's
     * display fields" — so a statement asking for one column got the target's whole display group, with the
     * joins to fetch it.
     *
     * <p>The cost was not theoretical. {@code select documentLine.document.id from Attendance} compiled to
     * SEVENTEEN columns, two of those joins unused, and among the columns Document's special_needs, comment
     * and request — a public endpoint reading members' health and accessibility notes in order to count
     * rows. Measured across the tree: 43 of 241 compilable statements shed columns here, several of them
     * dropping from 49 to 28 or 50 to 34.
     *
     * <p>Nothing needs the bare term. The factorized dot built below already carries the left, and the join
     * key itself is emitted by DotSqlCompiler, which reads the left independently of this.
     *
     * <p><b>What still loads a display group</b>, deliberately: a bare foreign key the caller actually wrote
     * — {@code select documentLine.document} still brings Document's group, and {@code select label} still
     * brings a Label's nine languages, including through a path such as {@code item.label}. What stops is
     * the SECOND level: a display group's own foreign keys no longer load theirs, so
     * {@code select organization from Event} now loads organization.id, type.id and organization.name and
     * not OrganizationType's name. Every display expression that reads a nested field keeps it —
     * Site.icon's {@code itemFamily.code}, Rate's {@code currency.symbol}, Organization.icon's
     * {@code type.id} — because those are named by the expression and collected as its own terms.
     */
    @Override
    public void collect(CollectOptions options) {
        if (!options.factorizeLeftDot()) {
            left.collect(options);
            right.collect(options);
        } else {
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
                return new Call<>(function.getName(), Dot.dot(call.getOperand(), getRight(), isOuterJoin()).expandLeft(), call.getOrderBy(), call.isDistinct());
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
