package dev.webfx.stack.routing.uirouter.activity.view.impl;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import dev.webfx.stack.routing.uirouter.activity.uiroute.impl.UiRouteActivityContextBase;
import dev.webfx.stack.routing.uirouter.activity.view.ViewActivityContext;
import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;

/**
 * @author Bruno Salmon
 */
public class ViewActivityContextBase
        <THIS extends ViewActivityContextBase<THIS>>

        extends UiRouteActivityContextBase<THIS>
        implements ViewActivityContext<THIS> {

    protected ViewActivityContextBase(ActivityContext parentContext, ActivityContextFactory<THIS> contextFactory) {
        super(parentContext, contextFactory);
    }

    private final Property<Node> nodeProperty = new SimpleObjectProperty<>();
    @Override
    public Property<Node> nodeProperty() {
        return nodeProperty;
    }

    private final Property<Node> mountNodeProperty = new SimpleObjectProperty<>();
    @Override
    public Property<Node> mountNodeProperty() {
        return mountNodeProperty;
    }

}
