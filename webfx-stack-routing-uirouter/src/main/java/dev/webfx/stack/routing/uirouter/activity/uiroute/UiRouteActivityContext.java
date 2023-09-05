package dev.webfx.stack.routing.uirouter.activity.uiroute;

import dev.webfx.platform.ast.json.ReadOnlyJsonObject;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.routing.activity.impl.elementals.activeproperty.ActivePropertyActivityContext;
import dev.webfx.stack.routing.uirouter.UiRouter;

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

    ReadOnlyJsonObject getParams();

    default <T> T getParameter(String key) { return getParams().get(key); }

    String getRoutingPath();

}
