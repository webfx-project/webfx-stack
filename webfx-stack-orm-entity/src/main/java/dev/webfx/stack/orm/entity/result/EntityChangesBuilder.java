package dev.webfx.stack.orm.entity.result;

import dev.webfx.platform.async.Handler;
import dev.webfx.platform.util.collection.HashList;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.result.impl.EntityChangesImpl;

import java.util.Collection;

/**
 * @author Bruno Salmon
 */
public final class EntityChangesBuilder {

    private EntityResultBuilder rsb;
    private Collection<EntityId> deletedEntities;
    private boolean hasChanges;
    private Handler<Boolean> hasChangesHandler; // used by EntityBindings only

    private EntityChangesBuilder() {}

    public void addDeletedEntityId(EntityId id) {
        if (id.isNew()) {
            cancelEntityChanges(id);
        } else {
            if (deletedEntities == null)
                deletedEntities = new HashList<>(); // Hash for uniqueness and List for keeping sequence order
            deletedEntities.add(id);
        }
        updateHasChanges();
    }

    public void addInsertedEntityId(EntityId id) {
        if (id.isNew())
            addFieldChange(id, null, null);
        updateHasChanges();
    }

    public void addUpdatedEntityId(EntityId id) {
        if (!id.isNew())
            addFieldChange(id, null, null);
        updateHasChanges();
    }

    public boolean hasEntityId(EntityId id) {
        return rsb != null && rsb.hasEntityId(id);
    }

    public boolean addFieldChange(EntityId id, Object fieldId, Object fieldValue) {
        boolean fieldChanged = rsb().setFieldValue(id, fieldId, fieldValue);
        updateHasChanges();
        return fieldChanged;
    }

    public void removeFieldChange(EntityId id, Object fieldId) {
        if (rsb != null)
            rsb.unsetFieldValue(id, fieldId);
        updateHasChanges();
    }

    public void cancelEntityChanges(EntityId id) {
        if (deletedEntities != null)
            deletedEntities.remove(id);
        if (rsb != null)
            rsb.removeEntityId(id);
        updateHasChanges();
    }

    public void clear() {
        rsb = null;
        deletedEntities = null;
        updateHasChanges();
    }

    public boolean hasChanges() {
        return deletedEntities != null && !deletedEntities.isEmpty() || rsb != null && !rsb.isEmpty();
    }

    private void updateHasChanges() {
        if (hasChanges != hasChanges()) {
            hasChanges = !hasChanges;
            if (hasChangesHandler != null)
                hasChangesHandler.handle(hasChanges);
        }
    }

    private EntityResultBuilder rsb() {
        if (rsb == null)
            rsb = EntityResultBuilder.create();
        return rsb;
    }

    public EntityChanges build() {
        return new EntityChangesImpl(rsb == null ? null : rsb.build(), deletedEntities);
    }

    public static EntityChangesBuilder create() {
        return new EntityChangesBuilder();
    }

    // method meant to be used by EntityBindings only

    public void setHasChangesHandler(Handler<Boolean> hasChangesHandler) {
        this.hasChangesHandler = hasChangesHandler;
    }
}
