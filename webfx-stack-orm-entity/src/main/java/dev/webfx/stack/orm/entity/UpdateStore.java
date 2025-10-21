package dev.webfx.stack.orm.entity;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.impl.DynamicEntity;
import dev.webfx.stack.orm.entity.impl.UpdateStoreImpl;
import dev.webfx.stack.orm.entity.result.EntityChanges;

/**
 * @author Bruno Salmon
 */
public interface UpdateStore extends EntityStore {

    default <E extends Entity> E insertEntity(Object domainClassId) {
        return insertEntity(domainClassId, null);
    }

    default <E extends Entity> E insertEntity(Object domainClassId, Object primaryKey) {
        return insertEntity(getEntityId(domainClassId, primaryKey));
    }

    <E extends Entity> E insertEntity(EntityId entityId);

    default <E extends Entity> E updateEntity(E entity) {
        if (entity == null)
            return null;
        E updatedEntity = updateEntity(entity.getId());
        if (updatedEntity instanceof DynamicEntity && entity != updatedEntity) {
            DynamicEntity dynamicEntity = (DynamicEntity) updatedEntity;
            dynamicEntity.setUnderlyingEntity(entity);
        }
        return updatedEntity;
    }

    <E extends Entity> E updateEntity(EntityId entityId);

    default <E extends Entity> E updateEntity(Object domainClassId, Object primaryKey) {
        return updateEntity(getEntityId(domainClassId, primaryKey));
    }

    default void deleteEntity(Entity entity) {
        if (entity != null)
            deleteEntity(entity.getId());
    }

    void deleteEntity(EntityId entityId);

    default void deleteEntity(Object domainClassId, Object primaryKey) {
        deleteEntity(getEntityId(domainClassId, primaryKey));
    }

    EntityChanges getEntityChanges();

    boolean hasChanges();

    void cancelChanges();

    void markChangesAsCommitted();

    void setSubmitScope(DataScope submitScope);

    Future<SubmitChangesResult> submitChanges(SubmitArgument... initialSubmits);

    // Factory

    static UpdateStore create(DataSourceModel dataSourceModel) {
        return new UpdateStoreImpl(dataSourceModel);
    }

    static UpdateStore create() {
        return create(DataSourceModelService.getDefaultDataSourceModel());
    }

    static UpdateStore createAbove(EntityStore underlyingStore) {
        return new UpdateStoreImpl(underlyingStore);
    }
}