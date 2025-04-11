package dev.webfx.stack.orm.reactive.mapping.entities_to_visual;

import dev.webfx.extras.visual.VisualColumn;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.orm.domainmodel.formatter.ValueFormatter;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.reactive.entities.entities_to_grid.EntityColumnBuilder;

/**
 * @author Bruno Salmon
 */
public final class VisualEntityColumnBuilder<E extends Entity> extends EntityColumnBuilder<E> {

    private VisualColumn visualColumn;

    public VisualEntityColumnBuilder<E> setVisualColumn(VisualColumn visualColumn) {
        this.visualColumn = visualColumn;
        return this;
    }

    @Override
    public VisualEntityColumnBuilder<E> setExpressionDefinition(String expressionDefinition) {
        return (VisualEntityColumnBuilder<E>) super.setExpressionDefinition(expressionDefinition);
    }

    @Override
    public VisualEntityColumnBuilder<E> setExpression(Expression<E> expression) {
        return (VisualEntityColumnBuilder<E>) super.setExpression(expression);
    }

    @Override
    public VisualEntityColumnBuilder<E> setLabel(Object label) {
        return (VisualEntityColumnBuilder<E>) super.setLabel(label);
    }

    @Override
    public VisualEntityColumnBuilder<E> setDisplayFormatter(ValueFormatter displayFormatter) {
        return (VisualEntityColumnBuilder<E>) super.setDisplayFormatter(displayFormatter);
    }

    @Override
    public VisualEntityColumnBuilder<E> setJson(ReadOnlyAstObject json) {
        return (VisualEntityColumnBuilder<E>) super.setJson(json);
    }

    @Override
    public VisualEntityColumn<E> build() {
        return new VisualEntityColumnImpl<>(expressionDefinition, expression, label, displayFormatter, visualColumn, json);
    }
}
