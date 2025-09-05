package dev.webfx.stack.orm.entity;

import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.Booleans;
import dev.webfx.platform.util.Numbers;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainModel;
import dev.webfx.stack.orm.entity.lciimpl.EntityDomainReader;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Select;
import dev.webfx.stack.orm.expression.terms.function.Call;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author Bruno Salmon
 */
public final class Entities {

    public static EntityId getId(Entity entity) {
        return entity == null ? null : entity.getId();
    }

    public static EntityId getId(Object entityInstanceOrId) {
        return entityInstanceOrId == null ? null : entityInstanceOrId instanceof Entity ? getId((Entity) entityInstanceOrId) : (EntityId) entityInstanceOrId;
    }

    public static Object getPrimaryKey(Entity entity) {
        return entity == null ? null : entity.getPrimaryKey();
    }

    public static Object getPrimaryKey(EntityId entityId) {
        return entityId == null ? null : entityId.getPrimaryKey();
    }

    public static Object getPrimaryKey(Object o) {
        return o instanceof Entity ? ((Entity) o).getPrimaryKey() : o instanceof EntityId ? ((EntityId) o).getPrimaryKey() : o;
    }

    public static boolean isNew(Entity entity) {
        return entity != null && entity.isNew();
    }

    public static boolean isNotNew(Entity entity) {
        return entity != null && !entity.isNew();
    }

    public static boolean sameId(Entity e1, Entity e2) {
        return e1 == e2 || e1 != null && e2 != null && e1.getId().equals(e2.getId());
    }

    public static boolean samePrimaryKey(Object o1, Object o2) {
        // We always return false if one or both objects are null, because this method is to compare real entities
        if (o1 == null || o2 == null)
            return false;
        Object pk1 = getPrimaryKey(o1), pk2 = getPrimaryKey(o2);
        return Numbers.identicalObjectsOrNumberValues(pk1, pk2);
    }

    public static <E extends Entity> List<E> filter(List<E> entityList, String filterExpression) {
        if (Collections.isEmpty(entityList))
            return Collections.emptyList();
        DomainClass domainClass = entityList.get(0).getDomainClass();
        return filter(entityList, filterExpression, domainClass.getId(), domainClass.getDomainModel());
    }

    public static <E extends Entity> List<E> filter(List<E> entityList, String filterExpression, Object entityClassId, DomainModel domainModel) {
        return Collections.isEmpty(entityList) ? Collections.emptyList() : filter(entityList, domainModel.parseExpression(filterExpression, entityClassId));
    }

    public static <E extends Entity> List<E> filter(List<E> entityList, Expression<E> filterExpression) {
        return filter(entityList, e -> Booleans.isTrue(e.evaluate(filterExpression)));
    }

    public static <E extends Entity> List<E> filter(List<E> entityList, Predicate<? super E> predicate) {
        return Collections.filter(entityList, predicate);
    }

    public static <E extends Entity> List<E> select(List<E> entityList, String select) {
        if (Collections.isEmpty(entityList))
            return Collections.emptyList();
        return select(entityList, select, entityList.get(0).getDomainClass().getDomainModel());
    }

    public static <E extends Entity> List<E> select(List<E> entityList, String select, DomainModel domainModel) {
        if (Collections.isEmpty(entityList))
            return Collections.emptyList();
        return select(entityList, domainModel.parseSelect(select));
    }

    public static <E extends Entity> List<E> select(List<E> entityList, Select<E> select) {
        return filter(entityList, select.getWhere());
    }

    public static <E extends Entity> List<E> orderBy(List<E> entityList, Expression<E>... orderExpressions) {
        if (Collections.isEmpty(entityList))
            return entityList;
        EntityStore store = entityList.get(0).getStore();
        return Call.orderBy(entityList, new EntityDomainReader<>(store), orderExpressions);
    }

    public static <E extends Entity> List<E> orderBy(List<E> entityList, String... orderExpressions) {
        if (Collections.isEmpty(entityList))
            return entityList;
        DomainClass domainClass = entityList.get(0).getDomainClass();
        return orderBy(entityList, Arrays.map(orderExpressions, oe -> domainClass.parseExpression(oe.startsWith("order by") ? oe : "order by " + oe), Expression[]::new));
    }
}