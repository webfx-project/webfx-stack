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
import dev.webfx.stack.orm.entity.result.*;
import javafx.beans.binding.BooleanExpression;

/**
 * @author Bruno Salmon
 */
public final class UpdateStoreImpl extends EntityStoreImpl implements UpdateStore {

    private final EntityChangesBuilder changesBuilder = EntityChangesBuilder.create();
    private EntityResultBuilder previousValues;
    private DataScope submitScope;

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

    boolean updateEntity(EntityId id, Object domainFieldId, Object value, Object previousValue) {
        if (Objects.areEquals(value, previousValue))
            return false;

        if (previousValues != null && Objects.areEquals(value, previousValues.getFieldValue(id, domainFieldId))) {
            changesBuilder.removeFieldChange(id, domainFieldId);
            return true;
        }

        if (!changesBuilder.hasEntityId(id)) { // TODO: remove if no side effect
            // return false // Commented for Audio Recording & Video Settings (otherwise subsequent changes after submit are ignored)
            Console.log("[UpdateStoreImpl] WARNING: Changing a field on an entity not known by the changesBuilder");
        }

        boolean firstFieldChange = updateEntity(id, domainFieldId, value);
        if (firstFieldChange)
            rememberPreviousEntityFieldValue(id, domainFieldId, previousValue);
        return firstFieldChange;
    }

    boolean updateEntity(EntityId id, Object domainFieldId, Object value) {
        return changesBuilder.addFieldChange(id, domainFieldId, value);
    }

    void rememberPreviousEntityFieldValue(EntityId id, Object domainFieldId, Object value) {
        if (previousValues == null)
            previousValues = EntityResultBuilder.create();
        previousValues.setFieldValue(id, domainFieldId, value);
    }

    @Override
    public void setSubmitScope(DataScope submitScope) {
        this.submitScope = submitScope;
    }

    @Override
    public Future<Batch<SubmitResult>> submitChanges(SubmitArgument... initialSubmits) {
        try {
            EntityChangesToSubmitBatchGenerator.BatchGenerator updateBatchGenerator = EntityChangesToSubmitBatchGenerator.createSubmitBatchGenerator(getEntityChanges(), dataSourceModel, submitScope, initialSubmits);
            Batch<SubmitArgument> argBatch = updateBatchGenerator.generate();
            Console.log("Executing submit batch " + Arrays.toStringWithLineFeeds(argBatch.getArray()));
            return SubmitService.executeSubmitBatch(argBatch).compose(resBatch -> {
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
    public BooleanExpression hasChangesProperty() {
        return changesBuilder.hasChangesProperty();
    }

    @Override
    public void cancelChanges() {
        changesBuilder.clear();
        restorePreviousValues();
        previousValues = null;
    }

    private void restorePreviousValues() {
        if (previousValues != null) {
            EntityResult rs = previousValues.build();
            for (EntityId id : rs.getEntityIds()) {
                Entity entity = getEntity(id);
                for (Object fieldId : rs.getFieldIds(id))
                    entity.setFieldValue(fieldId, rs.getFieldValue(id, fieldId));
            }
            previousValues = null;
        }
    }

    @Override
    public void markChangesAsCommitted() {
        previousValues = null;
        changesBuilder.clear();
    }
}
