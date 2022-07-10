package dev.webfx.stack.framework.shared.orm.entity.impl;

import dev.webfx.stack.framework.shared.orm.entity.Entities;
import dev.webfx.stack.framework.shared.orm.entity.Entity;
import dev.webfx.stack.framework.shared.orm.entity.EntityList;
import dev.webfx.stack.framework.shared.orm.entity.EntityStore;
import dev.webfx.platform.shared.util.collection.Collections;
import dev.webfx.stack.framework.shared.orm.expression.Expression;

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
    public void orderBy(Expression<E>... orderExpressions) {
        Entities.orderBy(this, orderExpressions);
    }

    @Override
    public String toString() {
        return Collections.toStringWithLineFeeds(this);
    }
}
