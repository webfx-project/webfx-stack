package dev.webfx.stack.orm.entity.result.impl;

import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.UpdateStore;
import dev.webfx.stack.orm.entity.result.EntityChanges;
import dev.webfx.stack.orm.entity.result.EntityResult;

import java.util.Collection;

/**
 * @author Bruno Salmon
 */
public final class EntityChangesImpl implements EntityChanges {

    private final EntityResult insertedUpdatedEntities;
    private final Collection<EntityId> deletedEntities;
    private final UpdateStore updateStore; // Optional, used to sort delete entities if provided

    public EntityChangesImpl(EntityResult insertedUpdatedEntities, Collection<EntityId> deletedEntities, UpdateStore updateStore) {
        this.insertedUpdatedEntities = insertedUpdatedEntities;
        this.deletedEntities = deletedEntities;
        this.updateStore = updateStore;
    }

    @Override
    public EntityResult getInsertedUpdatedEntityResult() {
        return insertedUpdatedEntities;
    }

    @Override
    public Collection<EntityId> getDeletedEntityIds() {
        return deletedEntities;
    }

    @Override
    public UpdateStore getUpdateStore() {
        return updateStore;
    }
}
