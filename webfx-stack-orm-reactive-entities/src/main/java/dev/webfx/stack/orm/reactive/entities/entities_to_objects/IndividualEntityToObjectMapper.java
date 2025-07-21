package dev.webfx.stack.orm.reactive.entities.entities_to_objects;

import dev.webfx.stack.orm.entity.Entity;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Bruno Salmon
 */
public interface IndividualEntityToObjectMapper<E extends Entity, O> {

    O getMappedObject();

    void onEntityChangedOrReplaced(E entity);

    void onEntityRemoved(E entity);

    static <E extends Entity, O, V> Function<E, IndividualEntityToObjectMapper<E, O>> createFactory(Supplier<V> viewFactory, BiConsumer<V, E> viewEntitySetter, Function<V, O> viewObjectGetter) {
        return e -> {
            IndividualEntityToObjectMapper<E, O> mapper = create(viewFactory, viewEntitySetter, viewObjectGetter);
            mapper.onEntityChangedOrReplaced(e);
            return mapper;
        };
    }

    private static <E extends Entity, O, V> IndividualEntityToObjectMapper<E, O> create(Supplier<V> viewFactory, BiConsumer<V, E> viewEntitySetter, Function<V, O> viewObjectGetter) {
        return new IndividualEntityToObjectMapper<>() {

            private final V view = viewFactory.get();

            @Override
            public O getMappedObject() {
                return viewObjectGetter.apply(view);
            }

            @Override
            public void onEntityChangedOrReplaced(E entity) {
                viewEntitySetter.accept(view, entity);
            }

            @Override
            public void onEntityRemoved(E entity) {}
        };
    }

    static <E extends Entity, O> Function<E, IndividualEntityToObjectMapper<E, O>> factory(Function<E, O> entityToObjectMapper) {
        return e -> new IndividualEntityToObjectMapper<>() {

            O object = entityToObjectMapper.apply(e);

            @Override
            public O getMappedObject() {
                return object;
            }

            @Override
            public void onEntityChangedOrReplaced(E entity) {
                // Is this allowed for this simple factory meant to be immutable?
                object = entityToObjectMapper.apply(e);
            }

            @Override
            public void onEntityRemoved(E entity) {}
        };
    }

}
