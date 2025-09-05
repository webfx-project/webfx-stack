package dev.webfx.stack.orm.reactive.entities.entities_to_objects;

import dev.webfx.stack.orm.reactive.entities.dql_to_entities.ReactiveEntitiesMapper;
import dev.webfx.stack.orm.reactive.entities.dql_to_entities.ReactiveEntitiesMapperAPI;
import javafx.collections.ObservableList;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.kit.util.properties.ObservableLists;

import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class ReactiveObjectsMapper<E extends Entity, T> implements ReactiveEntitiesMapperAPI<E, ReactiveObjectsMapper<E,T>> {

    private final ReactiveEntitiesMapper<E> reactiveEntitiesMapper;
    private Function<E, ? extends IndividualEntityToObjectMapper<E, T>> entityToObjectMapperFactory;

    public ReactiveObjectsMapper(ReactiveEntitiesMapper<E> reactiveEntitiesMapper) {
        this.reactiveEntitiesMapper = reactiveEntitiesMapper;
    }

    @Override
    public ReactiveEntitiesMapper<E> getReactiveEntitiesMapper() {
        return reactiveEntitiesMapper;
    }


    public ReactiveObjectsMapper<E,T> setIndividualEntityToObjectMapperFactory(Function<E, ? extends IndividualEntityToObjectMapper<E, T>> entityToObjectMapperFactory) {
        this.entityToObjectMapperFactory = entityToObjectMapperFactory;
        return this;
    }

    public ReactiveObjectsMapper<E,T> storeMappedObjectsInto(ObservableList<T> objects) {
        ObservableEntitiesToObjectsMapper<E, ? extends IndividualEntityToObjectMapper<E, T>> entitiesToObjectsMapper
            = new ObservableEntitiesToObjectsMapper<>(getObservableEntities(), entityToObjectMapperFactory,
            (e, m) -> m.onEntityChangedOrReplaced(e),
            (e, m) -> m.onEntityRemoved(e)
        );
        ObservableLists.bindConverted(objects, entitiesToObjectsMapper.getMappedObjects(), IndividualEntityToObjectMapper::getMappedObject);
        return this;
    }


    /*==================================================================================================================
      ======================================= Classic static factory API ===============================================
      ================================================================================================================*/

    public static <E extends Entity, T> ReactiveObjectsMapper<E,T> create(ReactiveEntitiesMapper<E> reactiveEntitiesMapper) {
        return new ReactiveObjectsMapper<>(reactiveEntitiesMapper);
    }

    /*==================================================================================================================
      ===================================== Shortcut static factory API ================================================
      ================================================================================================================*/

    public static <E extends Entity, T> ReactiveObjectsMapper<E,T> createReactiveChain() {
        return create(ReactiveEntitiesMapper.createReactiveChain());
    }

    public static <E extends Entity, T> ReactiveObjectsMapper<E,T> createReactiveChain(Object mixin) {
        return create(ReactiveEntitiesMapper.createReactiveChain(mixin));
    }

    public static <E extends Entity, T> ReactiveObjectsMapper<E,T> createPushReactiveChain() {
        return create(ReactiveEntitiesMapper.createPushReactiveChain());
    }

    public static <E extends Entity, T> ReactiveObjectsMapper<E,T> createPushReactiveChain(Object mixin) {
        return create(ReactiveEntitiesMapper.createPushReactiveChain(mixin));
    }
}
