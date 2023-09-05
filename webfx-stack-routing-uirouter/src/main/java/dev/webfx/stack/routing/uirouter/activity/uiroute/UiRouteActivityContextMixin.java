package dev.webfx.stack.routing.uirouter.activity.uiroute;

import dev.webfx.platform.ast.json.ReadOnlyJsonObject;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.routing.activity.impl.elementals.activeproperty.ActivePropertyActivityContextMixin;
import dev.webfx.stack.routing.uirouter.UiRouter;

/**
 * @author Bruno Salmon
 */
public interface UiRouteActivityContextMixin
        <C extends UiRouteActivityContext<C>>

        extends ActivePropertyActivityContextMixin<C>,
        UiRouteActivityContext<C> {

    @Override
    default UiRouter getUiRouter() { return getActivityContext().getUiRouter(); }

    @Override
    default BrowsingHistory getHistory() { return getActivityContext().getHistory(); }

    @Override
    default ReadOnlyJsonObject getParams() { return getActivityContext().getParams(); }

    @Override
    default <T> T getParameter(String key) {
        return getActivityContext().getParameter(key);
    }

    @Override
    default String getRoutingPath() {
        return getActivityContext().getRoutingPath();
    }

}
