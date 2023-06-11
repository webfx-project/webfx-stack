package dev.webfx.stack.orm.reactive.mapping.entities_to_visual;

import dev.webfx.extras.visual.HasVisualResultProperty;
import dev.webfx.extras.visual.VisualResult;
import dev.webfx.extras.visual.VisualSelection;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.reactive.entities.dql_to_entities.ReactiveEntitiesMapper;
import dev.webfx.stack.orm.reactive.entities.dql_to_entities.ReactiveEntitiesMapperAPI;
import javafx.beans.property.Property;

import java.util.List;

public interface ReactiveVisualMapperAPI<E extends Entity, THIS> extends ReactiveEntitiesMapperAPI<E, THIS> {

    ReactiveVisualMapper<E> getReactiveVisualMapper();

    @Override
    default ReactiveEntitiesMapper<E> getReactiveEntitiesMapper() {
        return getReactiveVisualMapper().getReactiveEntitiesMapper();
    }

    default List<E> getSelectedEntities() {
        return getReactiveVisualMapper().getSelectedEntities();
    }

    default E getSelectedEntity() {
        return getReactiveVisualMapper().getSelectedEntity();
    }

    default Property<VisualResult> visualResultProperty() {
        return getReactiveVisualMapper().visualResultProperty();
    }

    default THIS visualizeResultInto(HasVisualResultProperty hasVisualResultProperty) {
        getReactiveVisualMapper().visualizeResultInto(hasVisualResultProperty);
        return (THIS) this;
    }

    default THIS visualizeResultInto(Property<VisualResult> visualResultProperty) {
        getReactiveVisualMapper().visualizeResultInto(visualResultProperty);
        return (THIS) this;
    }

    default THIS setVisualSelectionProperty(Property<VisualSelection> visualSelectionProperty) {
        getReactiveVisualMapper().setVisualSelectionProperty(visualSelectionProperty);
        return (THIS) this;
    }

    default THIS setVisualNullEntity(E visualNullEntity) {
        getReactiveVisualMapper().setVisualNullEntity(visualNullEntity);
        return (THIS) this;
    }

    default E getVisualNullEntity() {
        return getReactiveVisualMapper().getVisualNullEntity();
    }

}
