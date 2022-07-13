package dev.webfx.stack.routing.uirouter.activity.view;

import javafx.scene.Node;
import dev.webfx.stack.routing.activity.impl.elementals.activeproperty.ActivePropertyActivity;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;

/**
 * @author Bruno Salmon
 */
public interface ViewActivity
        <C extends UiRouteActivityContext<C>>

        extends ActivePropertyActivity<C> {

    Node buildUi();

}
