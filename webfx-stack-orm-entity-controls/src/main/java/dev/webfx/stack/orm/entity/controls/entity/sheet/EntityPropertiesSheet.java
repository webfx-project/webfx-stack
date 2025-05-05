package dev.webfx.stack.orm.entity.controls.entity.sheet;

import dev.webfx.extras.cell.renderer.ValueApplier;
import dev.webfx.extras.cell.renderer.ValueRenderer;
import dev.webfx.extras.cell.renderer.ValueRenderingContext;
import dev.webfx.extras.imagestore.ImageStore;
import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.type.Types;
import dev.webfx.extras.visual.*;
import dev.webfx.extras.visual.controls.grid.VisualGrid;
import dev.webfx.extras.visual.impl.VisualColumnImpl;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.formatter.ValueFormatter;
import dev.webfx.stack.orm.domainmodel.formatter.ValueParser;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.reactive.entities.entities_to_grid.EntityColumn;
import dev.webfx.stack.orm.reactive.mapping.entities_to_visual.VisualEntityColumn;
import dev.webfx.stack.orm.reactive.mapping.entities_to_visual.VisualEntityColumnFactory;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class EntityPropertiesSheet<E extends Entity> extends EntityUpdateDialog<E> {

    private static final VisualColumn LABEL_COLUMN = VisualColumn.create((value, context) -> {
        dev.webfx.extras.label.Label webfxExtrasLabel = (dev.webfx.extras.label.Label) value;
        Label label = new Label(null, ImageStore.createImageView(webfxExtrasLabel.getIconPath()));
        ValueApplier.applyTextValue(webfxExtrasLabel.getText(), label.textProperty());
        return label;
    });
    private static final VisualColumn VALUE_COLUMN = new VisualColumnImpl(null, null, null, null, VisualStyle.CENTER_STYLE, (value, context) -> (Node) value, null, null);
    private static final boolean TABLE_LAYOUT = true;

    private final VisualEntityColumn<E>[] entityColumns;
    private final ValueRenderingContext[] valueRenderingContexts;
    private final Node[] renderingNodes;
    private VisualEntityColumn<E>[] applicableEntityColumns;

    private EntityPropertiesSheet(E entity, String expressionColumns) {
        this((VisualEntityColumn<E>[]) VisualEntityColumnFactory.get().fromJsonArrayOrExpressionsDefinition(expressionColumns, entity.getDomainClass()));
        setEntity(entity);
    }

    private EntityPropertiesSheet(VisualEntityColumn<E>[] entityColumns) {
        this.entityColumns = entityColumns;
        valueRenderingContexts = Arrays.map(entityColumns, this::createValueRenderingContext, ValueRenderingContext[]::new);
        renderingNodes = new Node[entityColumns.length];
        // Temporary code
        //TextRenderer.SINGLETON.setTextFieldFactory(this::newMaterialTextField);
    }

    @Override
    Expression<? extends Entity> expressionToLoad() {
        return VisualEntityColumnFactory.get().toDisplayExpressionArray(entityColumns);
    }

    private ValueRenderingContext createValueRenderingContext(VisualEntityColumn<E> entityColumn) {
        Object labelKey = entityColumn.getVisualColumn().getLabel().getCode();
        DomainClass foreignClass = entityColumn.getForeignClass();
        ValueRenderingContext context;
        // Returning a standard ValueRenderingContext if the expression expresses just a value and not a foreign entity
        if (foreignClass == null)
            context = new ValueRenderingContext(entityColumn.isReadOnly(), labelKey, null, Types.isNumberType(entityColumn.getExpression().getType()) ? VisualStyle.RIGHT_STYLE.getTextAlign() : null);
        // Returning a EntityRenderingContext otherwise (in case of a foreign entity) which will be used by the EntityRenderer
        else
            context = new EntityRenderingContext(entityColumn.isReadOnly(), labelKey, null, entityColumn, () -> entity.getStore(), () -> dialogParent, this);
        FXProperties.runOnPropertyChange(() -> applyUiChangeOnEntity(entityColumn, context), context.getEditedValueProperty());
        return context;
    }

    @Override
    public void setEntity(E entity) {
        super.setEntity(entity);
        for (EntityColumn<E> entityColumn : entityColumns)
            entityColumn.parseExpressionDefinitionIfNecessary(entity.getDomainClass());
    }

    @Override
    Node buildNode() {
        if (!TABLE_LAYOUT)
            return new VBox(10);
        VisualGrid visualGrid = VisualGrid.createVisualGridWithTableSkin();
        visualGrid.setHeaderVisible(false);
        visualGrid.setFullHeight(true);
        visualGrid.setSelectionMode(SelectionMode.DISABLED);
        return visualGrid;
    }

    @Override
    void syncUiFromModel() {
        initDisplay();
        for (int i = 0, n = applicableEntityColumns.length; i < n; i++) {
            VisualEntityColumn entityColumn = applicableEntityColumns[i];
            Object modelValue = updateEntity.evaluate(castExpression(entityColumn.getExpression()));
            ValueFormatter displayFormatter = entityColumn.getDisplayFormatter();
            if (displayFormatter != null)
                modelValue = displayFormatter.formatValue(modelValue);
            int j = getApplicableValueRenderingContextIndex(entityColumn);
            ValueRenderingContext context = valueRenderingContexts[j];
            if (context.isReadOnly() || renderingNodes[j] == null) {
                // Using a standard value renderer in case of a value, or the EntityRenderer in case of a foreign entity
                ValueRenderer valueRenderer = entityColumn.getForeignClass() == null ? entityColumn.getVisualColumn().getValueRenderer() : EntityRenderer.SINGLETON;
                renderingNodes[j] = valueRenderer.renderValue(modelValue, context);
            }
            addExpressionRow(i, entityColumn, renderingNodes[j]);
        }
        applyDisplay();
    }

    private boolean isColumnApplicable(EntityColumn entityColumn) {
        Expression<E> applicableCondition = castExpression(entityColumn.getApplicableCondition());
        return applicableCondition == null || Boolean.TRUE.equals(updateEntity.evaluate(applicableCondition));
    }

    private Expression<E> castExpression(Expression expression) {
        return (Expression<E>) expression;
    }

    private int getApplicableValueRenderingContextIndex(EntityColumn applicableEntityColumns) {
        return Arrays.indexOf(entityColumns, applicableEntityColumns);
    }

    private VisualResultBuilder rsb;
    private List<Node> children;

    private void initDisplay() {
        applicableEntityColumns = java.util.Arrays.stream(entityColumns).filter(this::isColumnApplicable).toArray(VisualEntityColumn[]::new);
        if (TABLE_LAYOUT)
            rsb = new VisualResultBuilder(applicableEntityColumns.length, LABEL_COLUMN, VALUE_COLUMN);
        else
            children = new ArrayList<>();
    }

    private void addExpressionRow(int row, VisualEntityColumn entityColumn, Node renderedValueNode) {
        if (TABLE_LAYOUT) {
            rsb.setValue(row, 0, entityColumn.getVisualColumn().getLabel());
            rsb.setValue(row, 1, renderedValueNode);
        } else
            children.add(renderedValueNode);
    }

    private void applyDisplay() {
        if (TABLE_LAYOUT) {
            VisualResult rs = rsb.build();
            VisualGrid visualGrid = (VisualGrid) node;
            VisualResult oldRs = visualGrid.getVisualResult();
            int rowCount = rs.getRowCount();
            if (oldRs == null || oldRs.getRowCount() != rowCount)
                visualGrid.setVisualResult(rs);
            else
                for (int i = 0; i < rowCount; i++) {
                    if (rs.getValue(i, 1) != oldRs.getValue(i, 1)) {
                        visualGrid.setVisualResult(rs);
                        break;
                    }
                }
            // The following code is a workaround for the web version which doesn't compute correctly on first show the widths of the drop down buttons (if any present in the value column)
            if (oldRs == null) // => indicates first show
                UiScheduler.scheduleInAnimationFrame(() -> {
                    visualGrid.setVisualResult(null); // Resetting to null and then reestablishing the result set forces
                    visualGrid.setVisualResult(rs);   // the data grid to recompute all widths (correctly now)
                }, 2); // 2 frames is enough to make it work
        } else
            ((Pane) node).getChildren().setAll(children);
    }

    private void applyUiChangeOnEntity(EntityColumn entityColumn, ValueRenderingContext valueRenderingContext) {
        Object value = valueRenderingContext.getEditedValue();
        Expression<E> expression = castExpression(entityColumn.getExpression());
        // Checking if it is a formatted value
        ValueFormatter formatter = entityColumn.getDisplayFormatter();
        if (formatter != null) {
            // Parsing the value if applicable
            if (formatter instanceof ValueParser)
                value = ((ValueParser) formatter).parseValue(value);
            // Ignoring the new value if it renders the same formatted value as before (this is mainly to prevent
            // update a LocalDateTime if the formatter doesn't display the milliseconds)
            Object previousModelValue = entity.evaluate(expression);
            if (Objects.equals(formatter.formatValue(value), formatter.formatValue(previousModelValue)))
                value = previousModelValue;
        }
        // Empty strings are considered as null values for non-string expression
        if ("".equals(value) && expression.getType() != PrimType.STRING) {
            value = null;
        }
        updateEntity.setExpressionValue(expression, value);
        updateOkButton();
        syncUiFromModel();
    }

    public static <E extends Entity> EntityPropertiesSheet<E> editEntity(E entity, String expressionColumns, Pane parent) {
        if (entity == null)
            return null;
        EntityPropertiesSheet<E> sheet = new EntityPropertiesSheet<>(entity, expressionColumns);
        sheet.showAsDialog(parent);
        return sheet;
    }
}
