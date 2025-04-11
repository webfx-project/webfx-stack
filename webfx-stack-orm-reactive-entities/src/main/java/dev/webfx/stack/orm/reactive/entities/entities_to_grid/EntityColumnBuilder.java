package dev.webfx.stack.orm.reactive.entities.entities_to_grid;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.orm.domainmodel.formatter.ValueFormatter;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.expression.Expression;

/**
 * @author Bruno Salmon
 */
public class EntityColumnBuilder<E extends Entity> {

    protected String expressionDefinition;
    protected Expression<E> expression;
    protected Object label;
    protected ValueFormatter displayFormatter;
    protected ReadOnlyAstObject json;

    public EntityColumnBuilder<E> setExpressionDefinition(String expressionDefinition) {
        this.expressionDefinition = expressionDefinition;
        return this;
    }

    public EntityColumnBuilder<E> setExpression(Expression<E> expression) {
        this.expression = expression;
        return this;
    }

    public EntityColumnBuilder<E> setLabel(Object label) {
        this.label = label;
        return this;
    }

    public EntityColumnBuilder<E> setDisplayFormatter(ValueFormatter displayFormatter) {
        this.displayFormatter = displayFormatter;
        return this;
    }

    public EntityColumnBuilder<E> setJson(ReadOnlyAstObject json) {
        this.json = json;
        return this;
    }

    public EntityColumn<E> build() {
        return new EntityColumnImpl<>(expressionDefinition, expression, label, displayFormatter, json);
    }

}
