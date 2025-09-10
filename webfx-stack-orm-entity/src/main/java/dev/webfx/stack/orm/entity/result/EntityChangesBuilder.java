package dev.webfx.stack.orm.entity.result;

import dev.webfx.platform.util.collection.HashList;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.entity.EntityDomainClassIdRegistry;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.UpdateStore;
import dev.webfx.stack.orm.entity.result.impl.EntityChangesImpl;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class EntityChangesBuilder {

    private EntityResultBuilder rsb;
    private Collection<EntityId> deletedEntities;
    private boolean hasChanges;
    private Consumer<Boolean> hasChangesPropertyUpdater; // used by EntityBindings only
    private UpdateStore updateStore; // Optional, just used to sort the deleted entities when provided

    private EntityChangesBuilder() {}

    public EntityChangesBuilder addDeletedEntityId(EntityId id) {
        if (id.isNew()) {
            cancelEntityChanges(id);
        } else {
            if (deletedEntities == null)
                deletedEntities = new HashList<>(); // Hash for uniqueness and List for keeping sequence order
            deletedEntities.add(id);
        }
        return updateHasChanges();
    }

    public EntityChangesBuilder addInsertedEntityId(EntityId id) {
        if (id.isNew())
            addFieldChange(id, null, null);
        return updateHasChanges();
    }

    public EntityChangesBuilder addUpdatedEntityId(EntityId id) {
        if (!id.isNew())
            addFieldChange(id, null, null);
        return updateHasChanges();
    }

    public boolean hasEntityId(EntityId id) {
        return rsb != null && rsb.hasEntityId(id);
    }

    public boolean addFieldChange(EntityId id, Object fieldId, Object fieldValue) {
        boolean fieldChanged = rsb().setFieldValue(id, fieldId, fieldValue);
        updateHasChanges();
        return fieldChanged;
    }

    public EntityChangesBuilder removeFieldChange(EntityId id, Object fieldId) {
        if (rsb != null)
            rsb.unsetFieldValue(id, fieldId);
        return updateHasChanges();
    }

    public EntityChangesBuilder cancelEntityChanges(EntityId id) {
        if (deletedEntities != null)
            deletedEntities.remove(id);
        if (rsb != null)
            rsb.removeEntityId(id);
        return updateHasChanges();
    }

    public EntityChangesBuilder considerEntityIdRefactor(EntityId entityId, Object newPk) {
        if (deletedEntities != null && deletedEntities.remove(entityId)) {
            deletedEntities.add(EntityId.create(entityId.getDomainClass(), newPk));
        }
        if (rsb != null)
            rsb.considerEntityIdRefactor(entityId, newPk);
        return this;
    }

    public EntityChangesBuilder addFilteredEntityChanges(EntityChanges ec, Object domainClassId, Object... fieldIds) {
        return addFilteredEntityResult(ec.getInsertedUpdatedEntityResult(), domainClassId, fieldIds);
    }

    public EntityChangesBuilder addFilteredEntityResult(EntityResult er, Object domainClassId, Object... fieldIds) {
        if (er != null) {
            DomainClass domainClass = EntityDomainClassIdRegistry.getDomainClass(domainClassId);
            for (EntityId id : er.getEntityIds()) {
                if (domainClass.equals(id.getDomainClass())) {
                    for (Object fieldId : fieldIds) {
                        Object fieldValue = er.getFieldValue(id, fieldId);
                        if (fieldValue != null || er.getFieldIds(id).contains(fieldId))
                            addFieldChange(id, fieldId, fieldValue);
                    }
                }
            }
        }
        return this;
    }

    public EntityChangesBuilder clear() {
        rsb = null;
        deletedEntities = null;
        return updateHasChanges();
    }

    public boolean hasChanges() {
        return deletedEntities != null && !deletedEntities.isEmpty() || rsb != null && !rsb.isEmpty();
    }

    private EntityChangesBuilder updateHasChanges() {
        if (hasChanges != hasChanges()) {
            hasChanges = !hasChanges;
            if (hasChangesPropertyUpdater != null)
                hasChangesPropertyUpdater.accept(hasChanges);
        }
        return this;
    }

    public EntityChangesBuilder setUpdateStore(UpdateStore updateStore) {
        this.updateStore = updateStore;
        return this;
    }

    private EntityResultBuilder rsb() {
        if (rsb == null)
            rsb = EntityResultBuilder.create();
        return rsb;
    }

    public EntityChanges build() {
        return new EntityChangesImpl(rsb == null ? null : rsb.build(), deletedEntities, updateStore);
    }

    public static EntityChangesBuilder create() {
        return new EntityChangesBuilder();
    }

    // method is public but meant to be used by EntityBindings only

    public void setHasChangesPropertyUpdater(Consumer<Boolean> hasChangesPropertyUpdater) {
        this.hasChangesPropertyUpdater = hasChangesPropertyUpdater;
    }
}
