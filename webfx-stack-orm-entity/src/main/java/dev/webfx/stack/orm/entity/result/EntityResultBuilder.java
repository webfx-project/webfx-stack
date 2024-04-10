package dev.webfx.stack.orm.entity.result;

import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.result.impl.EntityResultImpl;
import dev.webfx.platform.util.collection.HashList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class EntityResultBuilder {

    private final List<EntityId> entityIds = new HashList<>();
    private final List<Map> entityFieldsMaps = new ArrayList<>();
    private int changedEntitiesCount;

    private EntityResultBuilder() {
    }

    public boolean setFieldValue(EntityId id, Object fieldId, Object fieldValue) {
        if (id.isNew() && !entityIds.contains(id))
            changedEntitiesCount++;
        Map fieldMap = entityFieldMap(id);
        if (fieldId != null && hasEntityNoChange(id, fieldMap))
            changedEntitiesCount++;
        boolean firstFieldValueSet = !fieldMap.containsKey(fieldId);
        fieldMap.put(fieldId, fieldValue);
        return firstFieldValueSet;
    }

    private boolean hasEntityNoChange(EntityId id, Map fieldMap) {
        return !id.isNew() && (fieldMap.isEmpty() || fieldMap.size() == 1 && fieldMap.containsKey(null));
    }

    public Object getFieldValue(EntityId id, Object fieldId) {
        Map fieldMap = entityFieldMap(id);
        return fieldMap.get(fieldId);
    }

    void unsetFieldValue(EntityId id, Object fieldId) {
        Map fieldMap = entityFieldMap(id);
        if (fieldId != null)
            fieldMap.remove(fieldId);
        else
            fieldMap.clear();
        if (hasEntityNoChange(id, fieldMap))
            changedEntitiesCount--;
    }

    boolean isEmpty() {
        return changedEntitiesCount == 0;
    }

    boolean hasEntityId(EntityId id) {
        return entityIds.contains(id);
    }

    boolean removeEntityId(EntityId id) {
        int entityIndex = entityIds.indexOf(id);
        if (entityIndex == -1)
            return false;
        entityFieldsMaps.remove(entityIndex);
        entityIds.remove(entityIndex);
        changedEntitiesCount--;
        return true;
    }

    private Map entityFieldMap(EntityId id) {
        int entityIndex = entityIds.indexOf(id);
        Map entityFieldsMap;
        if (entityIndex != -1)
            entityFieldsMap = entityFieldsMaps.get(entityIndex);
        else {
            entityIds.add(id);
            entityFieldsMaps.add(entityFieldsMap = new HashMap());
        }
        return entityFieldsMap;
    }

    public EntityResult build() {
        return new EntityResultImpl(entityIds, entityFieldsMaps);
    }

    public static EntityResultBuilder create() {
        return new EntityResultBuilder();
    }
}