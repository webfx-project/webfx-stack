package dev.webfx.stack.orm.entity;

import dev.webfx.platform.util.collection.Collections;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public interface EntityFactoryProvider<E extends Entity> {

    Class<E> entityClass();

    Object domainClassId();

    EntityFactory<E> entityFactory();

    static Collection<EntityFactoryProvider> getProvidedFactories() {
        return Collections.listOf(ServiceLoader.load(EntityFactoryProvider.class));
    }

}
