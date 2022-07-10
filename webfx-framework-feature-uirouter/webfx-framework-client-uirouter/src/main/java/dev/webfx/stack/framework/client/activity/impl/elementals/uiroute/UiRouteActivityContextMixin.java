package dev.webfx.stack.framework.client.activity.impl.elementals.uiroute;

import dev.webfx.stack.framework.client.activity.impl.elementals.activeproperty.ActivePropertyActivityContextMixin;
import dev.webfx.stack.framework.shared.router.session.Session;
import dev.webfx.stack.framework.client.ui.uirouter.UiRouter;
import dev.webfx.stack.framework.client.ui.uirouter.uisession.UiSession;
import dev.webfx.stack.framework.client.ui.uirouter.uisession.UiSessionMixin;
import dev.webfx.stack.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.platform.json.JsonObject;

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
