package dev.webfx.stack.orm.entity.impl;

import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.entity.EntityDomainClassIdRegistry;
import dev.webfx.stack.orm.entity.EntityId;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class EntityIdImpl implements EntityId {

    private final DomainClass domainClass;
    private final Object primaryKey;

    public EntityIdImpl(DomainClass domainClass, Object primaryKey) {
        this.domainClass = domainClass;
        this.primaryKey = primaryKey;
    }

    @Override
    public DomainClass getDomainClass() {
        return domainClass;
    }

    @Override
    public Object getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public boolean isNew() {
        return primaryKey instanceof Number && ((Number) primaryKey).intValue() < 0; // convention for new ids
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityIdImpl entityId = (EntityIdImpl) o;

        if (!Objects.equals(domainClass, entityId.domainClass)) return false;
        return Numbers.identicalObjectsOrNumberValues(primaryKey, entityId.primaryKey);

    }

    @Override
    public String toString() {
        return "ID[" + domainClass + ':' + primaryKey + ']';
    }

    @Override
    public int hashCode() {
        int result = domainClass != null ? domainClass.hashCode() : 0;
        result = 31 * result + (primaryKey != null ? primaryKey.hashCode() : 0);
        return result;
    }

    private static int newPk;

    public static EntityIdImpl create(Object domainClassId, Object primaryKey) {
        return new EntityIdImpl(EntityDomainClassIdRegistry.getDomainClass(domainClassId), primaryKey != null ? primaryKey : --newPk);
    }

    public static EntityIdImpl create(Object domainClassId) {
        return create(domainClassId, null);
    }
}
