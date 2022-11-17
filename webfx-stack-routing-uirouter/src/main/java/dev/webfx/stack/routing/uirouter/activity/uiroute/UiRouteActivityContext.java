package dev.webfx.stack.routing.uirouter.activity.uiroute;

import dev.webfx.stack.routing.activity.impl.elementals.activeproperty.ActivePropertyActivityContext;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.routing.uirouter.UiRouter;
import dev.webfx.stack.routing.uirouter.uisession.UiSession;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.json.JsonObject;

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
