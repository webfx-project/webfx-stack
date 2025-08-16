package dev.webfx.stack.orm.entity;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.Booleans;
import dev.webfx.platform.util.Numbers;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.util.time.Times;
import dev.webfx.platform.util.tuples.Pair;
import dev.webfx.stack.cache.CacheEntry;
import dev.webfx.stack.cache.CacheFuture;
import dev.webfx.stack.cache.CachePromise;
import dev.webfx.stack.cache.DefaultCache;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Dot;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Interface used to interact with an entity = a domain object which can persist in the database. Behind it can be a
 * POJO if a java class exists for that domain class or just a DynamicEntity which just acts as a flexible field values
 * container.
 *
 * @author Bruno Salmon
 */
public interface Entity {

    /**
     * @return the unique entity identifier
     */
    EntityId getId();

    /**
     * @return the domain class for the entity
     */
    default DomainClass getDomainClass() {
        return getId().getDomainClass();
    }

    /**
     * @return the primary key of the counterpart database record for this entity
     */
    default Object getPrimaryKey() {
        return getId().getPrimaryKey();
    }

    /**
     * @return true if the designated entity is not yet inserted in the database. In this case, the primary key is a
     * temporary but non null object that works to identify the in-memory newly created entity instance.
     */
    default boolean isNew() {
        return getId().isNew();
    }

    /**
     * @return the store that manages this entity
     */
    EntityStore getStore();

    /***
     * @param domainFieldId the domain field unique id in the domain model
     * @return the value currently stored in this entity field
     */
    Object getFieldValue(Object domainFieldId);

    boolean isFieldLoaded(Object domainFieldId);

    Collection<Object> getLoadedFields();

    /**
     * Return the field value as a boolean. If the type is not a boolean, this can result in runtime errors.
     */
    default Boolean getBooleanFieldValue(Object domainFieldId) { return Booleans.toBoolean(getFieldValue(domainFieldId)); }

    /**
     * Return the field value as a String. If the type is not a String, this can result in runtime errors.
     */
    default String getStringFieldValue(Object domainFieldId) { return Strings.toString(getFieldValue(domainFieldId)); }

    /**
     * Return the field value as a int. If the type is not a int, this can result in runtime errors.
     */
    default Integer getIntegerFieldValue(Object domainFieldId) { return Numbers.toInteger(getFieldValue(domainFieldId)); }

    /**
     * Return the field value as a long. If the type is not a long, this can result in runtime errors.
     */
    default Long getLongFieldValue(Object domainFieldId) { return Numbers.toLong(getFieldValue(domainFieldId)); }

    /**
     * Return the field value as a float. If the type is not a float, this can result in runtime errors.
     */
    default Float getFloatFieldValue(Object domainFieldId) { return Numbers.toFloat(getFieldValue(domainFieldId)); }

    /**
     * Return the field value as a double. If the type is not a double, this can result in runtime errors.
     */
    default Double getDoubleFieldValue(Object domainFieldId) { return Numbers.toDouble(getFieldValue(domainFieldId)); }

    /**
     * Return the field value as an Instant. If the type is not an instant, this can result in runtime errors.
     */
    default Instant getInstantFieldValue(Object domainFieldId) { return Times.toInstant(getFieldValue(domainFieldId)); }

    /**
     * Return the field value as a LocalDateTime. If the type is not an instant, this can result in runtime errors.
     */
    default LocalDateTime getLocalDateTimeFieldValue(Object domainFieldId) { return Times.toLocalDateTime(getFieldValue(domainFieldId)); }

    /**
     * Return the field value as a LocalDate. If the type is not an instant, this can result in runtime errors.
     */
    default LocalDate getLocalDateFieldValue(Object domainFieldId) { return Times.toLocalDate(getFieldValue(domainFieldId)); }

    /**
     * Set the value of an entity field
     * @param domainFieldId the domain field unique id in the domain model
     * @param value the value to store in this entity field
     */
    void setFieldValue(Object domainFieldId, Object value);

    void setForeignField(Object foreignFieldId, Object foreignFieldValue);

    EntityId getForeignEntityId(Object foreignFieldId);

    default <E extends Entity> E getForeignEntity(Object foreignFieldId) {
        return getStore().getEntity(getForeignEntityId(foreignFieldId));
    }

    // Expression API

    default <T> T evaluate(String expression) {
        return getStore().evaluateEntityExpression(this, expression);
    }

    default <E extends Entity, T> T evaluate(Expression<E> expression) {
        return getStore().evaluateEntityExpression((E) this, expression);
    }

    default <E extends Entity> void setExpressionValue(Expression<E> expression, Object value) {
        getStore().setEntityExpressionValue((E) this, expression, value);
    }

    default <E extends Entity> Expression<E> parseExpression(String expression) {
        return getStore().getDomainModel().parseExpression(expression, getDomainClass().getId());
    }

    default <T> Future<T> evaluateOnceLoaded(String expression) {
        return onExpressionLoaded(expression).map(ignored -> evaluate(expression));
    }

    default <E extends Entity> Future<Object> evaluateOnceLoaded(Expression<E> expression) {
        return onExpressionLoaded(expression).map(ignored -> evaluate(expression));
    }

    default <E extends Entity> Future<E> onExpressionLoaded(String expression) {
        return onExpressionLoadedWithCache((CacheEntry<Pair<QueryArgument, QueryResult>>) null, expression);
    }

    default <E extends Entity> Future<E> onExpressionLoadedWithCache(String cacheEntryKey, String expression) {
        return onExpressionLoadedWithCache(DefaultCache.getDefaultCacheEntry(cacheEntryKey), expression);
    }

    default <E extends Entity> Future<E> onExpressionLoadedWithCache(CacheEntry<Pair<QueryArgument, QueryResult>> cacheEntry, String expression) {
        if (expression == null)
            return Future.succeededFuture((E) this);
        try {
            return onExpressionLoadedWithCache(cacheEntry, parseExpression(expression));
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    default <E extends Entity> Future<E> onExpressionLoaded(Expression<E> expression) {
        return onExpressionLoadedWithCache((CacheEntry<Pair<QueryArgument, QueryResult>>) null, expression);
    }

    default <E extends Entity> CacheFuture<E> onExpressionLoadedWithCache(String cacheEntryKey, Expression<E> expression) {
        return onExpressionLoadedWithCache(DefaultCache.getDefaultCacheEntry(cacheEntryKey), expression);
    }

    default <E extends Entity> CacheFuture<E> onExpressionLoadedWithCache(CacheEntry<Pair<QueryArgument, QueryResult>> cacheEntry, Expression<E> expression) {
        Collection<Expression<E>> unloadedPersistentTerms = getUnloadedPersistentTerms(expression);
        if (unloadedPersistentTerms.isEmpty())
            return CacheFuture.succeededFuture((E) this);
        String dqlQuery = "select " + unloadedPersistentTerms.stream()
            .map(e -> e instanceof Dot ? ((Dot) e).expandLeft() : e)
            .map(Object::toString)
            .collect(Collectors.joining(",")) + " from " + getDomainClass().getName() + " where id=?";
        CachePromise<E> promise = new CachePromise<>();
        getStore().executeQueryWithCache(cacheEntry, dqlQuery, getPrimaryKey())
            .onFailure(promise::fail)
            .onCacheAndOrSuccessWithDetails((el, fromCache, sameAsCache) ->
                promise.emitValue((E) this, fromCache, sameAsCache));
        return promise.future();
    }

    default <E extends Entity> Collection<Expression<E>> getUnloadedPersistentTerms(Expression<E> expression) {
        return expression.collectPersistentTerms().stream()
                .filter(pt -> !isPersistentTermLoaded(pt))
                .collect(Collectors.toList());
    }

    default <E extends Entity> boolean isPersistentTermLoaded(Expression<E> persistentTerm) {
        if (persistentTerm instanceof DomainField)
            return isFieldLoaded(((DomainField) persistentTerm).getId());
        if (persistentTerm instanceof Dot) {
            Dot<E> dot = (Dot<E>) persistentTerm;
            if (!(dot.getLeft() instanceof DomainField))
                return false;
            Object leftFieldId = ((DomainField) dot.getLeft()).getId();
            if (!isFieldLoaded(leftFieldId))
                return false;
            Entity rightEntity = getForeignEntity(leftFieldId);
            return rightEntity != null && rightEntity.isPersistentTermLoaded(dot.getRight());
        }
        if (persistentTerm instanceof ExpressionArray)
            return Arrays.stream(((ExpressionArray<E>) persistentTerm).getExpressions()).allMatch(this::isPersistentTermLoaded);
        return evaluate(persistentTerm) != null;
    }

}
