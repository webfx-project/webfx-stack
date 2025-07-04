package dev.webfx.stack.orm.entity.lciimpl;

import dev.webfx.stack.orm.entity.Entities;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.extras.type.PrimType;

/**
 * @author Bruno Salmon
 */
public class EntityDomainReader<E extends Entity> implements DomainReader<E> {

    protected final EntityStore entityStore;

    public EntityDomainReader(EntityStore entityStore) {
        this.entityStore = entityStore;
    }

    @Override
    public E getDomainObjectFromId(Object id, Object src) {
        if (id instanceof Entity)
            return (E) id;
        E entity = entityStore.getEntity((EntityId) id);
        if (entity == null && src instanceof Entity)
            entity = ((Entity) src).getStore().getEntity((EntityId) id);
        return entity;
    }

    @Override
    public Object getDomainObjectId(Entity entity) {
        return Entities.getId(entity);
    }

    @Override
    public Object getDomainFieldValue(Entity entity, Object fieldId) {
        if (entity == null)
            return null;
        if (fieldId instanceof DomainField)
            fieldId = ((DomainField) fieldId).getId();
        return entity.getFieldValue(fieldId);
    }

    @Override
    public Object getParameterValue(String name) {
        return entityStore.getParameterValue(name);
    }

    @Override
    public Object prepareValueBeforeTypeConversion(Object value, PrimType type) {
        return Entities.getPrimaryKey(value);
    }
}
