package dev.webfx.stack.orm.entity;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.entity.impl.UpdateStoreImpl;
import dev.webfx.stack.orm.entity.result.EntityChanges;
import javafx.beans.binding.BooleanExpression;

/**
 * @author Bruno Salmon
 */
public interface UpdateStore extends EntityStore {

    default <E extends Entity> E insertEntity(Class<E> entityClass) {
        return insertEntity(entityClass, null);
    }

    default <E extends Entity> E insertEntity(Object domainClassId) {
        return insertEntity(domainClassId, null);
    }

    default <E extends Entity> E insertEntity(DomainClass domainClass) {
        return insertEntity(domainClass, null);
    }

    default <E extends Entity> E insertEntity(Class<E> entityClass, Object primaryKey) {
        return insertEntity(EntityDomainClassIdRegistry.getEntityDomainClassId(entityClass), primaryKey);
    }

    default <E extends Entity> E insertEntity(Object domainClassId, Object primaryKey) {
        return insertEntity(getDomainClass(domainClassId), primaryKey);
    }

    default <E extends Entity> E insertEntity(DomainClass domainClass, Object primaryKey) {
        return insertEntity(EntityId.create(domainClass, primaryKey));
    }

    <E extends Entity> E insertEntity(EntityId entityId);

    default <E extends Entity> E updateEntity(E entity) {
        updateEntity(entity.getId());
        return copyEntity(entity);
    }

    <E extends Entity> E updateEntity(EntityId entityId);

    default <E extends Entity> E updateEntity(DomainClass domainClass, Object primaryKey) {
        return updateEntity(EntityId.create(domainClass, primaryKey));
    }

    default <E extends Entity> E updateEntity(Object domainClassId, Object primaryKey) {
        return updateEntity(EntityId.create(getDomainClass(domainClassId), primaryKey));
    }

    default <E extends Entity> E updateEntity(Class<E> entityClass, Object primaryKey) {
        return updateEntity(EntityDomainClassIdRegistry.getEntityDomainClassId(entityClass), primaryKey);
    }

    default void deleteEntity(Entity entity) {
        if (entity != null)
            deleteEntity(entity.getId());
    }

    void deleteEntity(EntityId entityId);

    default void deleteEntity(DomainClass domainClass, Object primaryKey) {
        deleteEntity(EntityId.create(domainClass, primaryKey));
    }

    default void deleteEntity(Object domainClassId, Object primaryKey) {
        deleteEntity(EntityId.create(getDomainClass(domainClassId), primaryKey));
    }

    default void deleteEntity(Class<? extends Entity> entityClass, Object primaryKey) {
        deleteEntity(EntityDomainClassIdRegistry.getEntityDomainClassId(entityClass), primaryKey);
    }

    EntityChanges getEntityChanges();

    boolean hasChanges();

    BooleanExpression hasChangesProperty();

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