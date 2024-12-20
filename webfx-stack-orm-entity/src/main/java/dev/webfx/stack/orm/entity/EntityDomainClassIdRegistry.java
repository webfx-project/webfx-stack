package dev.webfx.stack.orm.entity;

import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainModel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class EntityDomainClassIdRegistry {

    private static final Map<Class<? extends Entity>, Object> entityDomainClassIds = new HashMap<>();

    public static <E extends Entity> void registerEntityDomainClassId(Class<E> entityClass, Object domainClassId) {
        entityDomainClassIds.put(entityClass, domainClassId);
    }

    public static Object getEntityDomainClassId(Class<? extends Entity> entityClass) {
        return entityDomainClassIds.get(entityClass);
    }

    // Utility methods

    public static DomainClass getDomainClass(Object domainClassId) {
        return getDomainClass(domainClassId, null);
    }

    public static DomainClass getDomainClass(Object domainClassId, DomainModel domainModel) {
        if (domainClassId instanceof DomainClass)
            return (DomainClass) domainClassId;
        if (domainModel == null)
            domainModel = DataSourceModelService.getDefaultDataSourceModel().getDomainModel();
        if (domainClassId.getClass().equals(Class.class))
            domainClassId = getEntityDomainClassId((Class<? extends Entity>) domainClassId);
        return domainModel.getClass(domainClassId);
    }

}