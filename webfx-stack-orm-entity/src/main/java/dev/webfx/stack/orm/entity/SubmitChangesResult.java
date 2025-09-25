package dev.webfx.stack.orm.entity;

import dev.webfx.platform.async.Batch;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.orm.entity.result.EntityChanges;
import dev.webfx.stack.orm.entity.result.EntityChangesBuilder;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Bruno Salmon
 */
@SuppressWarnings("unusable-by-js")
public record SubmitChangesResult(EntityChanges changes, Batch<SubmitResult> batch, Map<EntityId, Integer> newEntityIdIndexInBatch, Map<EntityId, Integer> newEntityIdIndexInGeneratedKeys) {

    public int getRowCount() {
        return batch.getArray()[0].getRowCount();
    }

    public Object getGeneratedKey() {
        return batch.getArray()[0].getGeneratedKeys()[0];
    }

    public void forEachIdWithGeneratedKey(BiConsumer<EntityId, Object> refactorer) {
        // Updating the Ids of the entities newly created with the generated keys returned by the database
        for (Map.Entry<EntityId, Integer> entry : newEntityIdIndexInBatch.entrySet()) {
            EntityId newEntityId = entry.getKey();
            int indexInBatch = entry.getValue();
            SubmitResult submitResult = batch.get(indexInBatch);
            int generatedKeyIndex = newEntityIdIndexInGeneratedKeys.getOrDefault(newEntityId, 0);
            Object generatedKey = submitResult.getGeneratedKeys()[generatedKeyIndex];
            refactorer.accept(newEntityId, generatedKey);
        }
    }

    public EntityChanges getCommittedChanges() {
        EntityChangesBuilder ecb = EntityChangesBuilder.create();
        ecb.addEntityChanges(changes);
        forEachIdWithGeneratedKey(ecb::considerEntityIdRefactor);
        return ecb.build();
    }
}
