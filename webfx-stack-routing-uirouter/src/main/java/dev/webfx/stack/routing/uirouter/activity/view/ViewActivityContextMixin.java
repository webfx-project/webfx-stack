package dev.webfx.stack.routing.uirouter.activity.view;

import javafx.beans.property.Property;
import javafx.scene.Node;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public interface ViewActivityContextMixin
        <C extends ViewActivityContext<C>>

        extends UiRouteActivityContextMixin<C>,
        ViewActivityContext<C> {

    @Override
    default Property<Node> nodeProperty() { return getActivityContext().nodeProperty(); }

    @Override
    default Property<Node> mountNodeProperty() { return getActivityContext().mountNodeProperty(); }

}
