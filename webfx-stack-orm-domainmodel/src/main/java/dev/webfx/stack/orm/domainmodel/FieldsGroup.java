package dev.webfx.stack.orm.domainmodel;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.dql.sqlcompiler.ExpressionSqlCompiler;
import dev.webfx.stack.orm.expression.terms.Symbol;

/**
 * @author Bruno Salmon
 */
public final class FieldsGroup<T> extends Symbol<T> {

    private final DomainClass domainClass;
    private String fieldsDefinition;

    static {
        ExpressionSqlCompiler.declareSymbolSubclasses(FieldsGroup.class);
    }

    public FieldsGroup(DomainClass domainClass, String name, String fieldsDefinition) {
        super(name, null, null);
        this.domainClass = domainClass;
        this.fieldsDefinition = fieldsDefinition;
    }

    @Override
    public Expression<T> getForwardingTypeExpression() {
        return getExpression();
    }

    @Override
    public Expression<T> getExpression() {
        if (expression == null) {
            expression = domainClass.parseExpression(fieldsDefinition);
            fieldsDefinition = null;
        }
        return expression;
    }

    /**
     * FieldsGroups must never be compiled to a single SQL expression. Their purpose is to load
     * individual persistent fields, which is handled by the persistent-terms path (branch 2).
     */
    @Override
    public boolean isExpressionSqlCompilable() {
        return false;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        return sb.append('<').append(name).append('>');
    }
}
