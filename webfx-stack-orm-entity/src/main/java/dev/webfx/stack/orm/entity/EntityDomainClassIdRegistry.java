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

    public static DomainClass getEntityDomainClass(Class<? extends Entity> entityClass) {
        return getEntityDomainClass(entityClass, null);
    }

    public static DomainClass getEntityDomainClass(Class<? extends Entity> entityClass, DomainModel domainModel) {
        return getDomainClass(getEntityDomainClassId(entityClass), domainModel);
    }

    public static DomainClass getDomainClass(Object domainClassId) {
        return getDomainClass(domainClassId, null);
    }

    public static DomainClass getDomainClass(Object domainClassId, DomainModel domainModel) {
        if (domainModel == null)
            domainModel = DataSourceModelService.getDefaultDataSourceModel().getDomainModel();
        return domainModel.getClass(domainClassId);
    }

}