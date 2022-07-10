package dev.webfx.stack.framework.client.activity.impl.elementals.uiroute;

import dev.webfx.stack.framework.client.activity.impl.elementals.activeproperty.ActivePropertyActivityContext;
import dev.webfx.stack.framework.shared.router.session.Session;
import dev.webfx.stack.framework.client.ui.uirouter.UiRouter;
import dev.webfx.stack.framework.client.ui.uirouter.uisession.UiSession;
import dev.webfx.stack.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.platform.json.JsonObject;

/**
 * @author Bruno Salmon
 */
public interface UiRouteActivityContext
        <THIS extends UiRouteActivityContext<THIS>>

        extends ActivePropertyActivityContext<THIS> {

    UiRouter getUiRouter();

    default BrowsingHistory getHistory() {
        return getUiRouter().getHistory();
    }

    JsonObject getParams();

    default <T> T getParameter(String key) { return getParams().get(key); }

    Session getSession();

    String getRoutingPath();

    default UiSession getUiSession() {
        return getUiRouter().getUiSession();
    }
}
