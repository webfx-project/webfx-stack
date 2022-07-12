package dev.webfx.stack.framework.client.activity.impl.elementals.uiroute.impl;

import dev.webfx.platform.util.Objects;
import dev.webfx.stack.framework.client.activity.impl.elementals.activeproperty.impl.ActivePropertyActivityBase;
import dev.webfx.stack.framework.client.activity.impl.elementals.uiroute.UiRouteActivityContext;
import dev.webfx.stack.framework.client.activity.impl.elementals.uiroute.UiRouteActivityContextMixin;

/**
 * @author Bruno Salmon
 */
public class UiRouteActivityBase
        <C extends UiRouteActivityContext<C>>

        extends ActivePropertyActivityBase<C>
        implements UiRouteActivityContextMixin<C> {

    private Object lastRefreshValue;

    @Override
    public void onResume() {
        // Doing it each time, in case the params have changed on a later resume
        updateContextParametersFromRoute();
        updateModelFromContextParameters();
        super.onResume(); // Making active
    }

    protected void updateContextParametersFromRoute() {
    }

    protected void updateModelFromContextParameters() {
        Object refreshValue = getParameter("refresh");
        if (!Objects.areEquals(refreshValue, lastRefreshValue)) {
            refreshDataOnActive();
            lastRefreshValue = refreshValue;
        }
    }

    protected void refreshDataOnActive() {
    }
}
