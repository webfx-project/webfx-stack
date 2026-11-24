package dev.webfx.stack.orm.expression.terms;

import dev.webfx.extras.type.Type;
import dev.webfx.stack.orm.expression.CollectOptions;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.stack.orm.expression.lci.DomainWriter;
import dev.webfx.stack.orm.expression.CollectOptions;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.stack.orm.expression.lci.DomainWriter;

/**
 * @author Bruno Salmon
 */
public class Symbol<T> extends AbstractExpression<T> {

    protected final String name;
    protected final Type type;
    protected Expression<T> expression;

    public Symbol(String name) {
        this(name, (Type) null);
    }

    public Symbol(String name, Type type) {
        this(name, type, null);
    }

    public Symbol(String name, Expression<T> expression) {
        this(name, expression.getType(), expression);
    }

    public Symbol(String name, Type type, Expression<T> expression) {
        super(9);
        this.name = name;
        this.type = type;
        this.expression = expression;
    }

    public String getName() {
        return name;
    }

    /**
     * A symbol that stands for an expression has that expression's precedence, not a symbol's.
     *
     * <p>Without this, a field defined as {@code person_age = null or person_age >= 18} reported the high
     * precedence of a plain name, so nothing parenthesised it and AND-ing a condition onto it produced
     * {@code a is null or a >= 18 and <condition>} — the {@code and} binding to the right disjunct only. Any
     * caller composing a condition onto a client's WHERE therefore silently lost it for half the rows.
     *
     * <p>{@link dev.webfx.stack.orm.expression.terms.function.InlineFunction} already does exactly this, for
     * exactly this reason, which is why a function condition was parenthesised correctly while a field that
     * expands to the same thing was not.
     */
    @Override
    public int getPrecedenceLevel() {
        // getExpression(), not the field: DomainField parses its body lazily on first access, so reading the
        // field here answered "no body" for every field that had not been touched yet - which is most of them
        // at the moment a condition is being composed.
        Expression<T> body = getExpression();
        return body == null ? super.getPrecedenceLevel() : body.getPrecedenceLevel();
    }

    public Expression<T> getExpression() {
        return expression;
    }

    /**
     * Returns false if this symbol's expression should NOT be compiled to SQL inline when
     * compileExpressions=true. Subclasses like FieldsGroup should override this to return false,
     * so their contained fields are always loaded as individual persistent columns rather than
     * being compiled into a single SQL expression.
     */
    public boolean isExpressionSqlCompilable() {
        return true;
    }

    @Override
    public Expression<T> getForwardingTypeExpression() {
        return type != null ? this : getExpression();
    }

    @Override
    public Type getType() {
        return type != null ? type : getExpression().getType();
    }

    @Override
    public Object evaluate(T domainObject, DomainReader<T> domainReader) {
        if (getExpression() != null)
            return getExpression().evaluate(domainObject, domainReader);
        return domainReader.getDomainFieldValue(domainObject, name);
    }

    @Override
    public boolean isEditable() {
        return getExpression() == null || getExpression().isEditable();
    }

    @Override
    public void setValue(T domainObject, Object value, DomainWriter<T> dataWriter) {
        if (getExpression() != null)
            getExpression().setValue(domainObject, value, dataWriter);
        else
            dataWriter.setDomainFieldValue(domainObject, name, value);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        return sb.append(name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public void collect(CollectOptions options) {
        if (getExpression() != null)
            getExpression().collect(options);
        else
            options.addTerm(this);
    }
}
