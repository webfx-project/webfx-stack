package dev.webfx.stack.orm.expression.terms;

import dev.webfx.extras.type.Type;
import dev.webfx.stack.orm.expression.CollectOptions;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.stack.orm.expression.lci.DomainWriter;

/**
 * @author Bruno Salmon
 */
public final class ParameterReference<T> extends AbstractExpression<T> {

    public final static ParameterReference<?> UNNAMED_PARAMETER_REFERENCE = new ParameterReference<>(null, null);

    private final int index; // index of this parameter captured from $ syntax (ex: $1, $2, ...), -1 for named parameters

    private final String name; // name of this parameter captured from ? syntax (ex: ?event, ?organization, ...)

    // From KBS2 but will probably be removed in the future
    private final Expression<T> rightDot; // ex: ?selectedEvent.name

    public ParameterReference(int index) {
        super(9);
        this.index = index;
        name = null;
        rightDot = null;
    }

    public ParameterReference(String name) {
        this(name, null);
    }

    public ParameterReference(String name, Expression<T> rightDot) {
        super(9);
        index = -1;
        this.name = name;
        this.rightDot = rightDot;
    }

    public String getName() {
        return name;
    }

    public Expression<T> getRightDot() {
        return rightDot;
    }

    public int getIndex() {
        return index;
    }

    private Object getParameterValue(DomainReader<T> domainReader) {
        return domainReader.getParameterValue(name);
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public Object evaluate(T domainObject, DomainReader<T> domainReader) {
        Object value = getParameterValue(domainReader);
        if (rightDot != null) {
            domainObject = domainReader.getDomainObjectFromId(value, domainObject);
            value = rightDot.evaluate(domainObject, domainReader);
        }
        return value;
    }

    @Override
    public void setValue(T domainObject, Object value, DomainWriter<T> dataWriter) {
        dataWriter.setParameterValue(name, value);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (index >= 1) {
            sb.append('$').append(index);
        } else if (name != null) {
            sb.append(isSearchParameter() ? '?' : ':').append(name);
            if (rightDot != null) {
                sb.append('.');
                boolean lowerRightPrecedence = rightDot.getPrecedenceLevel() < 8; // DOT precedence
                if (lowerRightPrecedence)
                    sb.append('(');
                rightDot.toString(sb);
                if (lowerRightPrecedence)
                    sb.append(')');
            }
        } else
            sb.append('?');
        return sb;
    }

    @Override
    public void collect(CollectOptions options) {
        if (options.includeParameter())
            options.addTerm(this);
    }

    // These methods are not great. TODO: investigate how we can remove their usage

    public boolean isClientOnlyParameter(boolean forSelectClause) {
        return name != null && (name.equals("lang") || forSelectClause && name.startsWith("selected") && getRightDot() == null);
    }

    public boolean isSearchParameter() {
        return name != null && name.toLowerCase().contains("search");
    }

}
