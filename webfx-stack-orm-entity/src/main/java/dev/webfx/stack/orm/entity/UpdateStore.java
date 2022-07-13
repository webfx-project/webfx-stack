package dev.webfx.stack.orm.entity;

import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.entity.impl.UpdateStoreImpl;
import dev.webfx.stack.orm.entity.result.EntityChanges;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.async.Batch;
import dev.webfx.stack.async.Future;

/**
 * @author Bruno Salmon
 */
public interface UpdateStore extends EntityStore {

    default <E extends Entity> E insertEntity(Class<E> entityClass) {
        return insertEntity(EntityDomainClassIdRegistry.getEntityDomainClassId(entityClass));
    }

    default <E extends Entity> E insertEntity(Object domainClassId) {
        return insertEntity(getDomainClass(domainClassId));
    }

    <E extends Entity> E insertEntity(DomainClass domainClass);

    default <E extends Entity> E updateEntity(E entity) {
        updateEntity(entity.getId());
        return copyEntity(entity);
    }

    <E extends Entity> E updateEntity(EntityId entityId);

    default void deleteEntity(Entity entity) {
        deleteEntity(entity.getId());
    }

    void deleteEntity(EntityId entityId);

    EntityChanges getEntityChanges();

    boolean hasChanges();

    void cancelChanges();

    void markChangesAsCommitted();

    void setSubmitScope(DataScope submitScope);

    Future<Batch<SubmitResult>> submitChanges(SubmitArgument... initialSubmits);

    // Factory

    static UpdateStore create(DataSourceModel dataSourceModel) {
        return new UpdateStoreImpl(dataSourceModel);
    }

    static UpdateStore createAbove(EntityStore underlyingStore) {
        return new UpdateStoreImpl(underlyingStore);
    }
}