package dev.webfx.stack.orm.entity.impl;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.Objects;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.db.submit.SubmitService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.UpdateStore;
import dev.webfx.stack.orm.entity.result.EntityChanges;
import dev.webfx.stack.orm.entity.result.EntityChangesBuilder;
import dev.webfx.stack.orm.entity.result.EntityChangesToSubmitBatchGenerator;
import dev.webfx.stack.orm.entity.result.EntityResult;

/**
 * @author Bruno Salmon
 */
public final class UpdateStoreImpl extends EntityStoreImpl implements UpdateStore {

    private final EntityChangesBuilder changesBuilder = EntityChangesBuilder.create();
    private DataScope submitScope;
    private Object hasChangesProperty; // managed by EntityBindings

    public UpdateStoreImpl(DataSourceModel dataSourceModel) {
        super(dataSourceModel);
    }

    public UpdateStoreImpl(EntityStore underlyingStore) {
        super(underlyingStore);
    }

    @Override
    public EntityChanges getEntityChanges() {
        return changesBuilder.build();
    }

    @Override
    public <E extends Entity> E insertEntity(EntityId entityId) {
        if (!entityId.isNew())
            throw new IllegalArgumentException("entityId must be new");
        E entity = createEntity(entityId);
        changesBuilder.addInsertedEntityId(entityId);
        return entity;
    }

    @Override
    public <E extends Entity> E updateEntity(EntityId entityId) {
        changesBuilder.addUpdatedEntityId(entityId);
        return getOrCreateEntity(entityId);
    }

    void onInsertedOrUpdatedEntityFieldChange(EntityId id, Object domainFieldId, Object value, Object underlyingValue, boolean isUnderlyingValueLoaded) {
        // If the user enters back the original value, we completely clear that field from the changes
        if (isUnderlyingValueLoaded && Objects.areEquals(value, underlyingValue)) {
            changesBuilder.removeFieldChange(id, domainFieldId);
        } else {
            changesBuilder.addFieldChange(id, domainFieldId, value);
        }
    }

    @Override
    public void setSubmitScope(DataScope submitScope) {
        this.submitScope = submitScope;
    }

    @Override
    public Future<Batch<SubmitResult>> submitChanges(SubmitArgument... initialSubmits) {
        try {
            EntityChangesToSubmitBatchGenerator.BatchGenerator updateBatchGenerator = EntityChangesToSubmitBatchGenerator.
                createSubmitBatchGenerator(getEntityChanges(), getDataSourceModel(), submitScope, initialSubmits);
            Batch<SubmitArgument> argBatch = updateBatchGenerator.generate();
            Console.log("Executing submit batch " + Arrays.toStringWithLineFeeds(argBatch.getArray()));
            return SubmitService.executeSubmitBatch(argBatch).compose(resBatch -> {
                // TODO: perf optimization: make these steps optional if not required by application code
                markChangesAsCommitted();
                updateBatchGenerator.applyGeneratedKeys(resBatch, this);
                return Future.succeededFuture(resBatch);
            });
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    @Override
    public void deleteEntity(EntityId entityId) {
        changesBuilder.addDeletedEntityId(entityId);
    }

    @Override
    public boolean hasChanges() {
        return changesBuilder.hasChanges();
    }

    @Override
    public void cancelChanges() {
        clearAllUpdatedValuesFromUpdateStore();
        changesBuilder.clear();
    }

    private void clearAllUpdatedValuesFromUpdateStore() {
        EntityChanges changes = changesBuilder.build();
        EntityResult insertedUpdatedEntityResult = changes.getInsertedUpdatedEntityResult();
        if (insertedUpdatedEntityResult != null) {
            for (EntityId entityId : insertedUpdatedEntityResult.getEntityIds()) {
                clearAllUpdatedValuesFromUpdatedEntity(entityId);
            }
        }
    }

    private void clearAllUpdatedValuesFromUpdatedEntity(EntityId entityId) {
        if (!entityId.isNew()) {
            Entity updateEntity = getEntity(entityId);
            if (updateEntity instanceof DynamicEntity) {
                ((DynamicEntity) updateEntity).clearAllFields();
            }
        }
    }

    @Override
    public void markChangesAsCommitted() {
        applyCommittedChangesToUnderlyingStore();
        changesBuilder.clear();
    }

    private void applyCommittedChangesToUnderlyingStore() {
        EntityStore underlyingStore = getUnderlyingStore();
        if (underlyingStore != null) {
            EntityChanges changes = changesBuilder.build();
            EntityResult insertedUpdatedEntityResult = changes.getInsertedUpdatedEntityResult();
            for (EntityId entityId : insertedUpdatedEntityResult.getEntityIds()) {
                Entity underlyingEntity = underlyingStore.getEntity(entityId);
                if (underlyingEntity != null) {
                    for (Object fieldId : insertedUpdatedEntityResult.getFieldIds(entityId)) {
                        if (fieldId != null) {
                            Object fieldValue = insertedUpdatedEntityResult.getFieldValue(entityId, fieldId);
                            underlyingEntity.setFieldValue(fieldId, fieldValue);
                        }
                    }
                }
                clearAllUpdatedValuesFromUpdatedEntity(entityId);
            }
        }
    }

    // methods meant to be used by EntityBindings only


    public EntityChangesBuilder getChangesBuilder() {
        return changesBuilder;
    }

    public void setHasChangesProperty(Object hasChangesProperty) {
        this.hasChangesProperty = hasChangesProperty;
    }

    public Object getHasChangesProperty() {
        return hasChangesProperty;
    }
}
