package dev.webfx.stack.framework.client.orm.reactive.mapping.entities_to_objects;

import dev.webfx.stack.framework.shared.orm.entity.Entity;

/**
 * @author Bruno Salmon
 */
public interface IndividualEntityToObjectMapper<E extends Entity, T> {

    T getMappedObject();

    void onEntityChangedOrReplaced(E entity);

    void onEntityRemoved(E entity);

}
