package dev.webfx.stack.orm.entity.controls.entity.sheet;

import dev.webfx.extras.cell.renderer.ValueRendererFactory;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.entity.controls.entity.selector.EntityButtonSelector;
import javafx.scene.Node;
import dev.webfx.stack.orm.reactive.entities.entities_to_grid.EntityColumn;
import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.extras.cell.renderer.ValueRenderer;
import dev.webfx.extras.cell.renderer.ValueRenderingContext;
import dev.webfx.stack.platform.json.Json;
import javafx.scene.layout.HBox;

/**
 * @author Bruno Salmon
 */
public final class EntityRenderer implements ValueRenderer {

    final static EntityRenderer SINGLETON = new EntityRenderer();

    @Override
    public Node renderValue(Object value, ValueRenderingContext context) {
        if (context.isReadOnly() && (value instanceof Entity || value == null)) {
            if (value == null)
                return new HBox();
            Entity entity = (Entity) value;
            Expression<Entity> renderingExpression = entity.getDomainClass().getForeignFields();
            ValueRenderer valueRenderer = ValueRendererFactory.getDefault().createValueRenderer(renderingExpression.getType());
            return valueRenderer.renderValue(entity.evaluate(renderingExpression), context);
        }
        // Expecting an Entity or EntityId for the value
        EntityId entityId = Entities.getId(value);
        // Expecting an EntityRenderingContext for the context
        EntityRenderingContext erc = (EntityRenderingContext) context;
        // Retrieving the entity store and the domain class id
        EntityStore store = erc.getEntityStore();
        Object domainClassId = erc.getEntityClass().getModelId();
        // Defining the json or class object to be passed to the entity button selector
        EntityColumn foreignFieldColumn = erc.getForeignFieldColumn();
        Object jsonOrClass = foreignFieldColumn == null ? domainClassId // just the class id if there is no foreign field column defined
            : Json.createObject() // Json object otherwise (most of the case) with both "class" and "columns" set
                .set("class", domainClassId)
                .set("alias", foreignFieldColumn.getForeignAlias())
                // We prefix the columns definition with "expr:=" to ensure that the parsing - done by EntityColumns.fromJsonArrayOrExpressionsDefinition() -
                // will work when foreign fields is an expression array (ex: "[icon,name]"), because in that case the
                // string is an expression definition and not a json array despite the brackets (the correct json array
                // string would be ['icon','name'] instead). So the prefix will remove that possible confusion.
                .set("columns", "expr:=" + foreignFieldColumn.getForeignColumns())
                .set("where", foreignFieldColumn.getForeignWhere())
                .set("orderBy", foreignFieldColumn.getForeignOrderBy())
                ;
        // Creating the entity button selector and setting the initial entity
        EntityButtonSelector<Entity> selector = new EntityButtonSelector<>(jsonOrClass, erc.getButtonFactory(), erc.getParentGetter(), store.getDataSourceModel());
        if (foreignFieldColumn != null) {
            String searchCondition = foreignFieldColumn.getForeignSearchCondition();
            if (searchCondition != null)
                selector.setSearchCondition(searchCondition);
        }
        selector.setReadOnly(context.isReadOnly());
        // Also setting the edited value property in the rendering context to be the id of the entity selected in the button selector
        context.bindConvertedEditedValuePropertyTo(selector.selectedItemProperty(), store.getEntity(entityId), Entities::getId, store::getEntity);
        return selector.getButton();
    }

    public static Node renderEntity(Entity entity) {
        return SINGLETON.renderValue(entity);
    }

    public static Node renderEntity(Entity entity, ValueRenderingContext context) {
        return SINGLETON.renderValue(entity, context);
    }
}
