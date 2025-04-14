package dev.webfx.stack.orm.reactive.mapping.entities_to_visual;

import dev.webfx.extras.cell.renderer.ValueRenderer;
import dev.webfx.extras.cell.renderer.ValueRendererRegistry;
import dev.webfx.extras.type.Type;
import dev.webfx.extras.type.Types;
import dev.webfx.extras.visual.VisualColumn;
import dev.webfx.extras.visual.VisualColumnBuilder;
import dev.webfx.extras.visual.VisualStyleBuilder;
import dev.webfx.stack.orm.reactive.entities.entities_to_grid.EntityColumnImpl;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.As;
import dev.webfx.stack.orm.expression.terms.UnaryExpression;
import dev.webfx.stack.orm.domainmodel.formatter.ValueFormatter;
import dev.webfx.platform.ast.ReadOnlyAstObject;

/**
 * @author Bruno Salmon
 */
public final class VisualEntityColumnImpl<E extends Entity> extends EntityColumnImpl<E> implements VisualEntityColumn<E> {

    private VisualColumn visualColumn;

    public VisualEntityColumnImpl(String expressionDefinition, Expression<E> expression, Object label, ValueFormatter displayFormatter, VisualColumn visualColumn, ReadOnlyAstObject json) {
        super(expressionDefinition, expression, label, displayFormatter, json);
        this.visualColumn = visualColumn;
    }

    @Override
    public String getName() {
        return getVisualColumn().getName();
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public VisualColumn getVisualColumn() {
        if (visualColumn == null) {
            Expression<?> topRightExpression = getTopRightExpression();
            if (topRightExpression instanceof As) {
                As<?> as = (As<?>) topRightExpression;
                topRightExpression = as.getOperand();
                if (label == null)
                    label = as.getAlias();
            }
            if (label == null) {
                label = topRightExpression;
                while (label instanceof UnaryExpression)
                    label = ((UnaryExpression<?>) label).getOperand();
            }
            Double prefWidth = null;
            if (topRightExpression instanceof DomainField) {
                int fieldPrefWidth = ((DomainField) topRightExpression).getPrefWidth();
                if (fieldPrefWidth > 0)
                    prefWidth = (double) fieldPrefWidth;
            }
            Type displayType = null;
            if (displayFormatter != null)
                displayType = displayFormatter.getFormattedValueType(); // May be null (ex: NoFormatter) => must be guessed from expression
            if (displayType == null) {
                if (getDisplayExpression() != expression)
                    topRightExpression = getTopRightExpression(displayExpression);
                displayType = topRightExpression.getType();
            }
/* Commented as causes a problem with the monitor page which expects numbers
            if (visualFormatter == null && Types.isNumberType(displayType))
                visualFormatter = NumberFormatter.SINGLETON;
*/
            String textAlign = null;
            ValueRenderer fxValueRenderer = null;
            String role = null;
            Double minWidth = null;
            if (json != null) {
                textAlign = json.getString("textAlign");
                String renderer = json.getString("renderer");
                if (renderer != null)
                    fxValueRenderer = ValueRendererRegistry.getValueRenderer(renderer);
                String collator = json.getString("collator");
                if (collator != null && fxValueRenderer == null)
                    fxValueRenderer = ValueRenderer.create(displayType, collator);
                role = json.getString("role");
                if (json.has("prefWidth"))
                    prefWidth = json.getDouble("prefWidth");
                if (json.has("minWidth"))
                    minWidth = json.getDouble("minWidth");
                //json = null;
            }
            if (textAlign == null) {
                Type type = getDisplayExpression().getType();
                textAlign = Types.isNumberType(type) ? "right" : Types.isBooleanType(type) ? "center" : null;
            }
            visualColumn = VisualColumnBuilder.create(label, displayType)
                    .setStyle(VisualStyleBuilder.create().setMinWidth(minWidth).setPrefWidth(prefWidth).setTextAlign(textAlign).build())
                    .setRole(role)
                    .setValueRenderer(fxValueRenderer)
                    .setSource(this)
                    .build();
        }
        return visualColumn;
    }

}
