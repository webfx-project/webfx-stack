package dev.webfx.stack.orm.entity.result;

import dev.webfx.platform.util.collection.HashList;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.result.impl.EntityChangesImpl;
import dev.webfx.stack.orm.entity.result.impl.EntityResultImpl;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.*;

/**
 * @author Bruno Salmon
 */
public final class EntityChangesBuilder {

    private EntityResultBuilder rsb;
    private Collection<EntityId> deletedEntities;
    private BooleanProperty hasChangesProperty; // lazy instantiation

    private EntityChangesBuilder() {}

    public EntityChangesBuilder addDeletedEntityId(EntityId id) {
        if (id.isNew()) {
            cancelEntityChanges(id);
        } else {
            if (deletedEntities == null)
                deletedEntities = new HashList<>(); // Hash for uniqueness and List for keeping sequence order
            deletedEntities.add(id);
        }
        updateHasChangesProperty();
        return this;
    }

    public EntityChangesBuilder addInsertedEntityId(EntityId id) {
        if (id.isNew())
            addFieldChange(id, null, null);
        updateHasChangesProperty();
        return this;
    }

    public EntityChangesBuilder addUpdatedEntityId(EntityId id) {
        if (!id.isNew())
            addFieldChange(id, null, null);
        updateHasChangesProperty();
        return this;
    }

    public boolean hasEntityId(EntityId id) {
        return rsb != null && rsb.hasEntityId(id);
    }

    public boolean addFieldChange(EntityId id, Object fieldId, Object fieldValue) {
        boolean fieldChanged = rsb().setFieldValue(id, fieldId, fieldValue);
        updateHasChangesProperty();
        return fieldChanged;
    }

    public EntityChangesBuilder removeFieldChange(EntityId id, Object fieldId) {
        if (rsb != null)
            rsb.unsetFieldValue(id, fieldId);
        updateHasChangesProperty();
        return this;
    }

    public EntityChangesBuilder cancelEntityChanges(EntityId id) {
        if (deletedEntities != null)
            deletedEntities.remove(id);
        if (rsb != null)
            rsb.removeEntityId(id);
        updateHasChangesProperty();
        return this;
    }

    public void clear() {
        rsb = null;
        deletedEntities = null;
    }

    public boolean hasChanges() {
        return deletedEntities != null && !deletedEntities.isEmpty() || rsb != null && !rsb.isEmpty();
    }

    public BooleanExpression hasChangesProperty() {
        if (hasChangesProperty == null)
            hasChangesProperty = new SimpleBooleanProperty(hasChanges());
        return hasChangesProperty;
    }

    private void updateHasChangesProperty() {
        if (hasChangesProperty != null)
            hasChangesProperty.set(hasChanges());
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
}
