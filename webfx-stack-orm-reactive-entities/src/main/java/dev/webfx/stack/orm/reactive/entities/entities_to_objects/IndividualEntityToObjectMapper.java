package dev.webfx.stack.orm.reactive.entities.entities_to_objects;

import dev.webfx.stack.orm.entity.Entity;

/**
 * @author Bruno Salmon
 */
public interface IndividualEntityToObjectMapper<E extends Entity, T> {

    T getMappedObject();

    void onEntityChangedOrReplaced(E entity);

    void onEntityRemoved(E entity);

}
