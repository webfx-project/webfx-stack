package dev.webfx.stack.orm.entity.result.impl;

import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.result.EntityResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public record EntityResultImpl(List<EntityId> entityIds, List<Map> entityFieldsMaps) implements EntityResult {

    public EntityResultImpl {
        if (entityIds.size() != entityFieldsMaps.size())
            throw new IllegalArgumentException("entityIds and entityFieldsMaps must have the same size");
    }

    @Override
    public Collection<EntityId> getEntityIds() {
        return entityIds;
    }

    @Override
    public Collection<Object> getFieldIds(EntityId id) {
        int entityIndex = entityIds.indexOf(id);
        return entityIndex == -1 ? Collections.EMPTY_LIST : entityFieldsMaps.get(entityIndex).keySet();
    }

    @Override
    public Object getFieldValue(EntityId id, Object fieldId) {
        int entityIndex = entityIds.indexOf(id);
        return entityIndex == -1 ? null : entityFieldsMaps.get(entityIndex).get(fieldId);
    }
}
