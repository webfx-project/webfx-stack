package dev.webfx.stack.orm.entity.impl;

import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.EntityList;
import dev.webfx.stack.orm.entity.EntityStore;

import java.util.ArrayList;

/**
 * @author Bruno Salmon
 */
public final class EntityListImpl<E extends Entity> extends ArrayList<E> implements EntityList<E> {

    private final Object listId;
    private final EntityStore store;

    public EntityListImpl(Object listId, EntityStore store) {
        this.listId = listId;
        this.store = store;
    }

    @Override
    public Object getListId() {
        return listId;
    }

    @Override
    public EntityStore getStore() {
        return store;
    }

    @Override
    public String toString() {
        return Collections.toStringCommaSeparatedWithBracketsAndLineFeeds(this);
    }
}
