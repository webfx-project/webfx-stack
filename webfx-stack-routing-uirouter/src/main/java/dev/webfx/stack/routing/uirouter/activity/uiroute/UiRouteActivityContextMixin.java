package dev.webfx.stack.routing.uirouter.activity.uiroute;

import dev.webfx.stack.routing.activity.impl.elementals.activeproperty.ActivePropertyActivityContextMixin;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.routing.uirouter.UiRouter;
import dev.webfx.stack.routing.uirouter.uisession.UiSession;
import dev.webfx.stack.routing.uirouter.uisession.UiSessionMixin;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.json.JsonObject;

/**
 * @author Bruno Salmon
 */
public interface UiRouteActivityContextMixin
        <C extends UiRouteActivityContext<C>>

        extends ActivePropertyActivityContextMixin<C>,
        UiRouteActivityContext<C>,
        UiSessionMixin {

    @Override
    default UiRouter getUiRouter() { return getActivityContext().getUiRouter(); }

    @Override
    default BrowsingHistory getHistory() { return getActivityContext().getHistory(); }

    @Override
    default JsonObject getParams() { return getActivityContext().getParams(); }

    @Override
    default Session getSession() {
        return getActivityContext().getSession();
    }

    @Override
    default <T> T getParameter(String key) {
        return getActivityContext().getParameter(key);
    }

    @Override
    default String getRoutingPath() {
        return getActivityContext().getRoutingPath();
    }

    @Override
    default UiSession getUiSession() {
        return getActivityContext().getUiSession();
    }
}
