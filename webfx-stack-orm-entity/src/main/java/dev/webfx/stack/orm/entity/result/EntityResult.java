package dev.webfx.stack.orm.entity.result;

import dev.webfx.stack.orm.entity.EntityId;

import java.util.Collection;

/**
 * @author Bruno Salmon
 */
public interface EntityResult {

    Collection<EntityId> getEntityIds();

    Collection<Object> getFieldIds(EntityId id);

    Object getFieldValue(EntityId id, Object fieldId);
}
