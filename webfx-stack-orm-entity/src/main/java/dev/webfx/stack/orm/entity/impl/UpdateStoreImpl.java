package dev.webfx.stack.orm.entity.impl;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.*;
import dev.webfx.stack.orm.entity.result.EntityChanges;
import dev.webfx.stack.orm.entity.result.EntityChangesBuilder;
import dev.webfx.stack.orm.entity.result.EntityChangesToSubmitBatchGenerator;
import dev.webfx.stack.orm.entity.result.EntityResult;

/**
 * @author Bruno Salmon
 */
public final class UpdateStoreImpl extends EntityStoreImpl implements UpdateStore {

    private final EntityChangesBuilder changesBuilder = EntityChangesBuilder.create().setUpdateStore(this);
    private DataScope submitScope;
    private Object hasChangesProperty; // managed by EntityBindings
    private boolean submitting;

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
        logWarningIfChangesDuringSubmit();
        E entity = createEntity(entityId);
        changesBuilder.addInsertedEntityId(entityId);
        return entity;
    }

    @Override
    public <E extends Entity> E updateEntity(EntityId entityId) {
        logWarningIfChangesDuringSubmit();
        changesBuilder.addUpdatedEntityId(entityId);
        return getOrCreateEntity(entityId);
    }

    void onInsertedOrUpdatedEntityFieldChange(EntityId id, Object domainFieldId, Object value, Object underlyingValue, boolean isUnderlyingValueLoaded) {
        logWarningIfChangesDuringSubmit();
        // If the user enters back the original value, we completely clear that field from the changes
        if (isUnderlyingValueLoaded && Numbers.identicalObjectsOrNumberValues(value, underlyingValue)) {
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
    public Future<SubmitChangesResult> submitChanges(SubmitArgument... initialSubmits) {
        try {
            EntityChanges changes = getEntityChanges();
            EntityChangesToSubmitBatchGenerator.BatchGenerator updateBatchGenerator = EntityChangesToSubmitBatchGenerator.
                createSubmitBatchGenerator(changes, getDataSourceModel(), submitScope, initialSubmits);
            Batch<SubmitArgument> argBatch = updateBatchGenerator.generate();
            Console.log("Executing submit batch " + Arrays.toStringWithLineFeeds(argBatch.getArray()));
            submitting = true;
            return SubmitService.executeSubmitBatch(argBatch).compose(resBatch -> {
                // TODO: perf optimization: make these steps optional if not required by application code
                markChangesAsCommitted();
                SubmitChangesResult result = new SubmitChangesResult(changes, resBatch, updateBatchGenerator.getNewEntityIdIndexInBatch(), updateBatchGenerator.getNewEntityIdIndexInGeneratedKeys());
                // Applying the generated keys to the entities in this store
                result.forEachIdWithGeneratedKey(this::applyEntityIdRefactor);
                submitting = false;
                return Future.succeededFuture(result);
            });
        } catch (Exception e) {
            submitting = false;
            return Future.failedFuture(e);
        }
    }

    @Override
    public void deleteEntity(EntityId entityId) {
        logWarningIfChangesDuringSubmit();
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
            if (insertedUpdatedEntityResult != null) {
                for (EntityId entityId : insertedUpdatedEntityResult.getEntityIds()) {
                    // Normally the entity that has been inserted or updated comes from the underlying store, and we can
                    // retrieve it from this store
                    Entity underlyingEntity = underlyingStore.getEntity(entityId);
                    // If not, however, this is probably because the user called updateEntity(entity) where entity comes
                    // from another store than the underlying store. In this case, we still try to apply the committed
                    // changes to that entity (that we consider like the underlying entity)
                    if (underlyingEntity == null && getEntity(entityId) instanceof DynamicEntity dynamicEntity)
                        underlyingEntity = dynamicEntity.getUnderlyingEntity();
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
    }

    private void logWarningIfChangesDuringSubmit() {
        if (submitting) {
            Console.warn("[UpdateStore] Making changes during submitChanges() is not yet supported, and leads to inconsistent UpdateStore state.");
            Console.error(new Exception("Please use this exception stacktrace to identify the faulty call"));
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
