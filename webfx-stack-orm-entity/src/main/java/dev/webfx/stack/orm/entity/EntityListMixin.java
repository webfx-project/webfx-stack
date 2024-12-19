package dev.webfx.stack.orm.entity;

import dev.webfx.platform.util.collection.ListMixin;

/**
 * @author Bruno Salmon
 */
public interface EntityListMixin<E extends Entity> extends EntityList<E>, ListMixin<E> {

    EntityList<E> getEntityList();

    default EntityList<E> getList() {
        return getEntityList();
    }

    @Override
    default Object getListId() {
        return getEntityList().getListId();
    }

    @Override
    default EntityStore getStore() {
        return getEntityList().getStore();
    }

}
