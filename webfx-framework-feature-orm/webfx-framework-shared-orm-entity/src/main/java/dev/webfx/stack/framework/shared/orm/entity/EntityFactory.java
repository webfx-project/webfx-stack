package dev.webfx.stack.framework.shared.orm.entity;

/**
 * @author Bruno Salmon
 */
public interface EntityFactory<E extends Entity> {

    E createEntity(EntityId id, EntityStore store);

}
