package dev.webfx.stack.orm.entity.impl;


import dev.webfx.platform.console.Console;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * @author Bruno Salmon
 */
public class DynamicEntity implements Entity {

    private EntityId id;
    private final EntityStore store;
    private Entity underlyingEntity;
    private final Map<Object /*fieldId*/, Object /*fieldValue*/> fieldValues = new HashMap<>();
    // fields used by EntityBindings only:
    private Map<Object/*fieldId*/, Object /*fieldProperty*/> fieldProperties; // lazy instantiation
    private static BiConsumer<Object/*fieldProperty*/, Object /*fieldValue*/> FIELD_PROPERTY_UPDATER;

    protected DynamicEntity(EntityId id, EntityStore store) {
        this.id = id;
        this.store = store;
        EntityStore underlyingStore = store == null ? null : store.getUnderlyingStore();
        underlyingEntity = underlyingStore != null ? underlyingStore.getEntity(id) : null;
    }

    public void setUnderlyingEntity(Entity underlyingEntity) { // meant to be called by UpdateStore.updateEntity() only
        this.underlyingEntity = underlyingEntity;
    }

    public Entity getUnderlyingEntity() {
        return underlyingEntity;
    }

    @Override
    public EntityId getId() {
        return id;
    }

    void refactorId(EntityId oldId, EntityId newId) {
        if (id.equals(oldId))
            id = newId;
        for (Map.Entry entry : fieldValues.entrySet())
            if (oldId.equals(entry.getValue()))
                entry.setValue(newId);
    }

    @Override
    public EntityStore getStore() {
        return store;
    }

    @Override
    public Object getFieldValue(Object domainFieldId) {
        Object value = fieldValues.get(domainFieldId);
        if (value == null && underlyingEntity != null && !fieldValues.containsKey(domainFieldId)) {
            return underlyingEntity.getFieldValue(domainFieldId);
        }
        return value;
    }

    @Override
    public boolean isFieldLoaded(Object domainFieldId) {
        if (fieldValues.containsKey(domainFieldId))
            return true;
        if (underlyingEntity != null)
            return underlyingEntity.isFieldLoaded(domainFieldId);
        return false;
    }

    @Override
    public Collection<Object> getLoadedFields() {
        Set<Object> loadFields = fieldValues.keySet();
        if (underlyingEntity != null) {
            loadFields = new HashSet<>(loadFields); // because ketSet() returns an immutable set
            loadFields.addAll(underlyingEntity.getLoadedFields());
        }
        return loadFields;
    }

    @Override
    public void setForeignField(Object foreignFieldId, Object foreignFieldValue) {
        EntityId foreignEntityId;
        if (foreignFieldValue == null)
            foreignEntityId = null;
        else if (foreignFieldValue instanceof EntityId entityId)
            foreignEntityId = entityId;
        else if (foreignFieldValue instanceof Entity entity) {
            foreignEntityId = entity.getId();
            if (entity.getStore() != store && store.getEntity(foreignEntityId) == null) {
                Entity newEntity = store.createEntity(foreignEntityId);
                if (newEntity instanceof DynamicEntity newDynamicEntity) {
                    newDynamicEntity.underlyingEntity = entity;
                } else { // Will probably never happen in practice as all entities are DynamicEntity
                    store.copyEntity(entity);
                    // Note: logging only entity id, not revealing the full entity that may contain sensitive data
                    Console.log("Warning: this foreign entity has been copied into the store otherwise it was not accessible: " + entity.getId());
                }
            }
        } else {
            Object foreignClass = getDomainClass().getForeignClass(foreignFieldId);
            foreignEntityId = getStore().getEntityId(foreignClass, foreignFieldValue);
        }
        setFieldValue(foreignFieldId, foreignEntityId);
    }

    @Override
    public EntityId getForeignEntityId(Object foreignFieldId) {
        Object value = getFieldValue(foreignFieldId);
        if (value instanceof EntityId)
            return (EntityId) value;
        return null;
    }

    @Override
    public <E extends Entity> E getForeignEntity(Object foreignFieldId) {
        E foreignEntity = Entity.super.getForeignEntity(foreignFieldId);
        if (foreignEntity == null && underlyingEntity != null)
            foreignEntity = underlyingEntity.getForeignEntity(foreignFieldId);
        return foreignEntity;
    }

    public void setFieldValue(Object domainFieldId, Object value) {
        boolean loadedValue = ThreadLocalEntityLoadingContext.isThreadLocalEntityLoading();
        fieldValues.put(domainFieldId, value); // TODO: what if it's a loaded value and previous value was not?
        if (!loadedValue && store instanceof UpdateStore) {
            Object underlyingValue = underlyingEntity != null ? underlyingEntity.getFieldValue(domainFieldId) : null;
            boolean isUnderlyingValueLoaded = underlyingValue != null || underlyingEntity != null && underlyingEntity.isFieldLoaded(domainFieldId);
            ((UpdateStoreImpl) store).onInsertedOrUpdatedEntityFieldChange(id, domainFieldId, value, underlyingValue, isUnderlyingValueLoaded);
        }
        if (FIELD_PROPERTY_UPDATER != null) {
            Object fieldProperty = getFieldProperty(domainFieldId);
            if (fieldProperty != null)
                FIELD_PROPERTY_UPDATER.accept(fieldProperty, value);
        }
    }


    public void copyAllFieldsFrom(Entity entity) {
        DynamicEntity dynamicEntity = (DynamicEntity) entity;
        fieldValues.putAll(dynamicEntity.fieldValues);
    }

    public void clearAllFields() {
        fieldValues.clear();
    }

    // Implementing equals() and hashCode() -- Ex: entities are used as keys in GanttLayout parent/grandparent cache

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicEntity that = (DynamicEntity) o;
        if (!id.equals(that.id))
            return false;
//        if (!fieldValues.equals(that.fieldValues))
//            return false;
        return underlyingEntity == null || that.underlyingEntity == null || Objects.equals(underlyingEntity, that.underlyingEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder(), true).toString();
    }

    private StringBuilder toString(StringBuilder sb, boolean pk) {
        if (pk)
            sb.append(id.getDomainClass()).append("(pk: ").append(id.getPrimaryKey());
        String separator = pk ? ", " : " | ";
        for (Map.Entry<?, ?> entry : fieldValues.entrySet()) { // ConcurrentModificationException observed
            sb.append(separator).append(entry.getKey()).append(": ").append(entry.getValue());
            separator = ", ";
        }
        if (underlyingEntity instanceof DynamicEntity) {
            ((DynamicEntity) underlyingEntity).toString(sb, false);
        }
        if (pk)
            sb.append(')');
        return sb;
    }

    // methods are public but meant to be used by EntityBindings only

    public Object getFieldProperty(Object fieldId) {
        return fieldProperties == null ? null : fieldProperties.get(fieldId);
    }

    public void setFieldProperty(Object fieldId, Object fieldProperty) {
        if (fieldProperties == null)
            fieldProperties = new HashMap<>();
        fieldProperties.put(fieldId, fieldProperty);
    }

    public static void setFieldPropertyUpdater(BiConsumer<Object, Object> fieldPropertyUpdater) {
        FIELD_PROPERTY_UPDATER = fieldPropertyUpdater;
    }
}
