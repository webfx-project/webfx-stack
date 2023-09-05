package dev.webfx.stack.orm.reactive.mapping.entities_to_visual;

import dev.webfx.extras.visual.VisualColumn;
import dev.webfx.stack.orm.reactive.entities.entities_to_grid.EntityColumnFactory;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.domainmodel.formatter.ValueFormatter;
import dev.webfx.platform.ast.json.ReadOnlyJsonObject;

/**
 * @author Bruno Salmon
 */
public interface VisualEntityColumnFactory extends EntityColumnFactory {

    VisualEntityColumnFactory DEFAULT = new VisualEntityColumnFactory() {};

    static VisualEntityColumnFactory get() {
        return DEFAULT;
    }

    default <E extends Entity> VisualEntityColumn<E> create(String expressionDefinition, Expression<E> expression, Object label, ValueFormatter displayFormatter, ReadOnlyJsonObject json) {
        return create(expressionDefinition, expression, label, displayFormatter, null, json);
    }

    default <E extends Entity> VisualEntityColumn<E> create(String expressionDefinition, Expression<E> expression, Object label, ValueFormatter displayFormatter, VisualColumn visualColumn, ReadOnlyJsonObject json) {
        return new VisualEntityColumnImpl<>(expressionDefinition, expression, label, displayFormatter, visualColumn, json);
    }

    default <E extends Entity> VisualEntityColumn<E> create(Expression<E> expression, VisualColumn visualColumn) {
        return create(null, expression, null, null, visualColumn, null);
    }

    @Override
    default <E extends Entity> VisualEntityColumn<E>[] createArray(int size) {
        return new VisualEntityColumn[size];
    }
}
