package dev.webfx.framework.shared.orm.entity.lciimpl;

import dev.webfx.framework.shared.orm.entity.Entity;
import dev.webfx.framework.shared.orm.entity.EntityStore;
import dev.webfx.framework.shared.orm.domainmodel.DomainField;
import dev.webfx.framework.shared.orm.expression.lci.DomainWriter;
import dev.webfx.framework.shared.orm.entity.UpdateStore;

/**
 * @author Bruno Salmon
 */
public final class EntityDomainWriter<E extends Entity> extends EntityDomainReader<E> implements DomainWriter<E> {

    public EntityDomainWriter(EntityStore entityStore) {
        super(entityStore);
    }

    @Override
    public void setDomainFieldValue(E entity, Object fieldId, Object fieldValue) {
        if (fieldId instanceof DomainField)
            fieldId = ((DomainField) fieldId).getId();
        if (entity.getStore() != entityStore && entityStore instanceof UpdateStore)
            entity = ((UpdateStore) entityStore).updateEntity(entity);
        entity.setFieldValue(fieldId, fieldValue);
    }

    @Override
    public void setParameterValue(String name, Object value) {
    }
}
