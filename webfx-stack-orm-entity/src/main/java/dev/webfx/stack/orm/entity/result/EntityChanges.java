package dev.webfx.stack.orm.entity.result;

import dev.webfx.stack.orm.entity.EntityId;

import java.util.Collection;

/**
 * @author Bruno Salmon
 */
public interface EntityChanges {

    EntityResult getInsertedUpdatedEntityResult();

    Collection<EntityId> getDeletedEntityIds();

}
