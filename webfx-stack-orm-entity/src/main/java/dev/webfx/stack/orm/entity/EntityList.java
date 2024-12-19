package dev.webfx.stack.orm.entity;

import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.orm.entity.impl.EntityListImpl;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Select;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Bruno Salmon
 */
public interface EntityList<E extends Entity> extends List<E>, HasEntityStore {

    Object getListId();

    default List<E> filter(String filterExpression) {
        return Entities.filter(this, filterExpression);
    }

    default List<E> filter(String filterExpression, Object entityClassId) {
        return Entities.filter(this, filterExpression, entityClassId, getDomainModel());
    }

    default List<E> filter(Expression<E> filterExpression) {
        return Entities.filter(this, filterExpression);
    }

    default List<E> filter(Predicate<? super E> predicate) {
        return Entities.filter(this, predicate);
    }

    default List<E> select(String select) {
        return Entities.select(this, select, getDomainModel());
    }

    default List<E> select(Select<E> select) {
        return Entities.select(this, select);
    }

    default List<E> orderBy(Expression<E>... orderExpressions) {
        return Entities.orderBy(this, orderExpressions);
    }

    default List<E> orderBy(String... orderExpressions) {
        return Entities.orderBy(this, orderExpressions);
    }


    // static factory methods

    static <E extends Entity> EntityList<E> create(Object listId, EntityStore store) {
        return new EntityListImpl<>(listId, store);
    }

    static <E extends Entity> EntityList<E> create(Object listId, EntityStore store, Collection<E> collections) {
        EntityList<E> entities = create(listId, store);
        Collections.setAll(entities, collections);
        return entities;
    }
}
