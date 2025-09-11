package dev.webfx.stack.orm.entity.binding;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.orm.entity.*;
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
        return (BooleanProperty) getFieldProperty(entity, fieldId, false, SimpleBooleanProperty::new);
    }

    public static StringProperty getStringFieldProperty(Entity entity, String fieldId) {
        return (StringProperty) getFieldProperty(entity, fieldId, false, SimpleStringProperty::new);
    }

    public static IntegerProperty getIntegerFieldProperty(Entity entity, String fieldId) {
        return (IntegerProperty) getFieldProperty(entity, fieldId, false, SimpleIntegerProperty::new);
    }

    public static DoubleProperty getDoubleFieldProperty(Entity entity, String fieldId) {
        return (DoubleProperty) getFieldProperty(entity, fieldId, false, SimpleDoubleProperty::new);
    }

    public static ObjectProperty<LocalDate> getLocalDateFieldProperty(Entity entity, String fieldId) {
        return (ObjectProperty<LocalDate>) getFieldProperty(entity, fieldId, false, SimpleObjectProperty::new);
    }

    public static ObjectProperty<EntityId> getForeignEntityIdProperty(Entity entity, String fieldId) {
        return (ObjectProperty<EntityId>) getFieldProperty(entity, fieldId, true, SimpleObjectProperty::new);
    }

    public static <E extends Entity> ObjectProperty<E> getForeignEntityProperty(Entity entity, String fieldId) {
        ObjectProperty<EntityId> foreignEntityIdProperty = getForeignEntityIdProperty(entity, fieldId);
        ObjectProperty<E> foreignEntityProperty = new SimpleObjectProperty<>();
        // Updating the foreign entity property when the foreign entity id property changes
        FXProperties.runNowAndOnPropertyChange(id -> foreignEntityProperty.set(entity.getStore().getOrCreateEntity(id)) , foreignEntityIdProperty);
        // Updating the foreign entity id property when the foreign entity property changes
        FXProperties.runOnPropertyChange(e -> foreignEntityIdProperty.set(Entities.getId(e)), foreignEntityProperty);
        return foreignEntityProperty;
    }

    private static Property<?> getFieldProperty(Entity entity, String fieldId, boolean foreignEntityId, Supplier<Property<?>> propertyFactory) {
        // Checking if that field property has already been instantiated
        DynamicEntity dynamicEntity = (DynamicEntity) entity;
        Property fieldProperty = (Property) dynamicEntity.getFieldProperty(fieldId);
        if (fieldProperty == null) { // if not, we create it and initialize it
            Property finalFieldProperty = fieldProperty = propertyFactory.get();
            // Setting its initial value
            fieldProperty.setValue(foreignEntityId ? entity.getForeignEntityId(fieldId) : entity.getFieldValue(fieldId));
            // Changes made on this property will be applied back to the entity
            fieldProperty.addListener(observable -> {
                if (foreignEntityId)
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
            List<EntityId> changedIds = new ArrayList<>(entityChanges.getEntityIds());
            List<EntityId> changedIdsToCreate = new ArrayList<>();
            // First pass: we apply the changed field values only on the entities already present in the store, but we
            // also memorize if those values point to a foreign entity not present in the store but present in the
            // changes, in which case we will force the creation of those entities in the store on the second pass.
            // Ex: if listening event.livestreamMessageLabel, the first pass may only set the EntityId for this field
            // but won't create the label itself.
            applyEntityChangesToRegisteredStore(entityChanges, entityStore, changedIds, changedIdsToCreate, false);
            // Second pass: we force the creation of the foreign entities detected in the first pass and apply the
            // changed field values to them, while continuing to detect and create more possible later foreign entities.
            // Ex: will create the Label with all its fields (if notified as well).
            applyEntityChangesToRegisteredStore(entityChanges, entityStore, changedIds, changedIdsToCreate, true);
        }
    }

    private static void applyEntityChangesToRegisteredStore(EntityResult entityChanges, EntityStore entityStore, List<EntityId> changedIds, List<EntityId> changedIdsToCreate, boolean secondPass) {
        List<EntityId> iteratingEntityIds = secondPass ? changedIdsToCreate : changedIds;
        for (int i = 0; i < iteratingEntityIds.size(); i++) {
            EntityId entityId = iteratingEntityIds.get(i);
            Entity entity = secondPass ? entityStore.getOrCreateEntity(entityId) : entityStore.getEntity(entityId);
            if (entity != null) {
                for (Object fieldId : entityChanges.getFieldIds(entityId)) {
                    Object fieldValue = entityChanges.getFieldValue(entityId, fieldId);
                    entity.setFieldValue(fieldId, fieldValue);
                    if (fieldValue instanceof EntityId id && !changedIdsToCreate.contains(id) && changedIds.contains(id) && entityStore.getEntity(id) == null)
                        changedIdsToCreate.add(id);
                }
            }
        }
    }
}
