package dev.webfx.stack.orm.entity.binding;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;
import dev.webfx.stack.orm.entity.impl.DynamicEntity;
import dev.webfx.stack.orm.entity.impl.UpdateStoreImpl;
import dev.webfx.stack.orm.entity.result.EntityChangesBuilder;
import dev.webfx.stack.orm.entity.result.EntityResult;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.*;
import javafx.scene.Node;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Bruno Salmon
 */
public final class EntityBindings {

    public static BooleanExpression hasChangesProperty(UpdateStore updateStore) {
        UpdateStoreImpl updateStoreImpl = (UpdateStoreImpl) updateStore;
        BooleanProperty hasChangesProperty = (BooleanProperty) updateStoreImpl.getHasChangesProperty();
        if (hasChangesProperty == null) {
            EntityChangesBuilder changesBuilder = updateStoreImpl.getChangesBuilder();
            hasChangesProperty = new SimpleBooleanProperty(changesBuilder.hasChanges()) {
                @Override
                protected void invalidated() {
                    get(); // For some reason, it's necessary to call get() here, otherwise the bindings depending on
                    // this property might not be updated 🤷
                }
            };
            changesBuilder.setHasChangesPropertyUpdater(hasChangesProperty::set);
        }
        return hasChangesProperty;
    }

    public static BooleanExpression hasNoChangesProperty(UpdateStore updateStore) {
        return hasChangesProperty(updateStore).not();
    }

    public static void disableNodesWhenUpdateStoreHasNoChanges(UpdateStore updateStore, Node... nodes) {
        BooleanExpression hasNoChanges = hasNoChangesProperty(updateStore);
        for (Node node : nodes)
            node.disableProperty().bind(hasNoChanges);

    }

    public static BooleanProperty getBooleanFieldProperty(Entity entity, String fieldId) {
        return (BooleanProperty) getFieldProperty(entity, fieldId, false, false, SimpleBooleanProperty::new);
    }

    public static StringProperty getStringFieldProperty(Entity entity, String fieldId) {
        return (StringProperty) getFieldProperty(entity, fieldId, false, false, SimpleStringProperty::new);
    }

    public static IntegerProperty getIntegerFieldProperty(Entity entity, String fieldId) {
        return (IntegerProperty) getFieldProperty(entity, fieldId, false, false, SimpleIntegerProperty::new);
    }

    public static DoubleProperty getDoubleFieldProperty(Entity entity, String fieldId) {
        return (DoubleProperty) getFieldProperty(entity, fieldId, false, false, SimpleDoubleProperty::new);
    }

    public static ObjectProperty<LocalDate> getLocalDateFieldProperty(Entity entity, String fieldId) {
        return (ObjectProperty<LocalDate>) getFieldProperty(entity, fieldId, false, false, SimpleObjectProperty::new);
    }

    public static ObjectProperty<EntityId> getForeignEntityIdProperty(Entity entity, String fieldId) {
        return (ObjectProperty<EntityId>) getFieldProperty(entity, fieldId, true, false, SimpleObjectProperty::new);
    }

    public static <E extends Entity> ObjectProperty<E> getForeignEntityProperty(Entity entity, String fieldId) {
        return (ObjectProperty<E>) getFieldProperty(entity, fieldId, false, true, SimpleObjectProperty::new);
    }

    private static Property<?> getFieldProperty(Entity entity, String fieldId, boolean foreignEntityId, boolean foreignEntity, Supplier<Property<?>> propertyFactory) {
        // Checking if that field property has already been instantiated
        DynamicEntity dynamicEntity = (DynamicEntity) entity;
        Property fieldProperty = (Property) dynamicEntity.getFieldProperty(fieldId);
        if (fieldProperty == null) { // if not, we create it and initialize it
            Property finalFieldProperty = fieldProperty = propertyFactory.get();
            // Setting its initial value
            fieldProperty.setValue(foreignEntityId ? entity.getForeignEntityId(fieldId) : foreignEntity ? entity.getForeignEntity(fieldId) : entity.getFieldValue(fieldId));
            // Changes made on this property will be applied back to the entity
            fieldProperty.addListener(observable -> {
                if (foreignEntityId || foreignEntity)
                    entity.setForeignField(fieldId, finalFieldProperty.getValue());
                else
                    entity.setFieldValue(fieldId, finalFieldProperty.getValue());
            });
            // And changes to the entity will be sent back to the property
            DynamicEntity.setFieldPropertyUpdater(EntityBindings::onEntityFieldValueChanged);
            // Memorizing this new field property into the entity
            dynamicEntity.setFieldProperty(fieldId, fieldProperty);
        }
        return fieldProperty;
    }

    private static void onEntityFieldValueChanged(Object fieldProperty, Object value) {
        // Checking it's not equals to prevent a possible bound exception if the change comes from a binding already (ex: i18n)
        FXProperties.setIfNotEquals((Property) fieldProperty, value);
    }

    private static final List<EntityStore> STORES_LISTENING_ENTITY_CHANGES = new ArrayList<>();

    public static void registerStoreForEntityChanges(EntityStore entityStore) {
        Collections.addIfNotContains(entityStore, STORES_LISTENING_ENTITY_CHANGES);
    }

    public static void unregisterStoreForEntityChanges(EntityStore entityStore) {
        STORES_LISTENING_ENTITY_CHANGES.remove(entityStore);
    }

    public static void applyEntityChangesToRegisteredStores(EntityResult entityChanges) {
        for (EntityStore entityStore : STORES_LISTENING_ENTITY_CHANGES) {
            for (EntityId entityId : entityChanges.getEntityIds()) {
                Entity entity = entityStore.getEntity(entityId);
                if (entity != null) {
                    for (Object fieldId : entityChanges.getFieldIds(entityId)) {
                        Object fieldValue = entityChanges.getFieldValue(entityId, fieldId);
                        entity.setFieldValue(fieldId, fieldValue);
                    }
                }
            }
        }
    }

}
