package dev.webfx.stack.orm.reactive.entities.dql_to_entities;

import dev.webfx.stack.orm.reactive.dql.query.ReactiveDqlQuery;
import dev.webfx.stack.orm.reactive.dql.query.ReactiveDqlQueryAPI;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.EntityList;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.entity.HasEntityStore;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public interface ReactiveEntitiesMapperAPI<E extends Entity, THIS> extends HasEntityStore, ReactiveDqlQueryAPI<E, THIS> {

    ReactiveEntitiesMapper<E> getReactiveEntitiesMapper();

    @Override
    default DataSourceModel getDataSourceModel() {
        return getReactiveDqlQuery().getDataSourceModel();
    }

    default EntityStore getStore() {
        return getReactiveEntitiesMapper().getStore();
    }

    default ReactiveDqlQuery<E> getReactiveDqlQuery() {
        return getReactiveEntitiesMapper().getReactiveDqlQuery();
    }

    default EntityList<E> getEntities() {
        return getReactiveEntitiesMapper().getEntities();
    }

    default EntityList<E> getCurrentEntities() {
        return getReactiveEntitiesMapper().getCurrentEntities();
    }

    default ObservableList<E> getObservableEntities() {
        return getReactiveEntitiesMapper().getObservableEntities();
    }

    default void refreshWhenActive() {
        getReactiveEntitiesMapper().refreshWhenActive();
    }

    default THIS setStore(EntityStore store) {
        getReactiveEntitiesMapper().setStore(store);
        return (THIS) this;
    }

    default THIS setListId(Object listId) {
        getReactiveEntitiesMapper().setListId(listId);
        return (THIS) this;
    }

    default THIS setRestrictedFilterList(List<E> restrictedFilterList) {
        getReactiveEntitiesMapper().setRestrictedFilterList(restrictedFilterList);
        return (THIS) this;
    }

    default THIS addEntitiesHandler(Consumer<EntityList<E>> entitiesHandler) {
        getReactiveEntitiesMapper().addEntitiesHandler(entitiesHandler);
        return (THIS) this;
    }

    default THIS removeEntitiesHandler(Consumer<EntityList<E>> entitiesHandler) {
        getReactiveEntitiesMapper().removeEntitiesHandler(entitiesHandler);
        return (THIS) this;
    }

    default THIS storeEntitiesInto(ObservableList<E> entities) {
        getReactiveEntitiesMapper().storeEntitiesInto(entities);
        return (THIS) this;
    }

    default THIS appendNullEntity(boolean first) {
        getReactiveEntitiesMapper().appendNullEntity(first);
        return (THIS) this;
    }

    default boolean isNullEntityAppended() {
        return getReactiveEntitiesMapper().isNullEntityAppended();
    }

}
