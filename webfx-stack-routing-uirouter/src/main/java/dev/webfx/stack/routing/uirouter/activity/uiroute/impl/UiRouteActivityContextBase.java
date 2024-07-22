package dev.webfx.stack.routing.uirouter.activity.uiroute.impl;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.windowhistory.WindowHistory;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.stack.routing.activity.ActivityContext;
import dev.webfx.stack.routing.activity.ActivityContextFactory;
import dev.webfx.stack.routing.activity.impl.ActivityContextBase;
import dev.webfx.stack.routing.uirouter.UiRouter;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;

/**
 * @author Bruno Salmon
 */
public class UiRouteActivityContextBase
        <THIS extends UiRouteActivityContextBase<THIS>>

        extends ActivityContextBase<THIS>
        implements UiRouteActivityContext<THIS> {

    private UiRouter uiRouter;
    private ReadOnlyAstObject params;
    private String routingPath;

    protected UiRouteActivityContextBase(ActivityContext parentContext, ActivityContextFactory<THIS> contextFactory) {
        super(parentContext, contextFactory);
    }

    public void setUiRouter(UiRouter uiRouter) {
        this.uiRouter = uiRouter;
    }

    @Override
    public UiRouter getUiRouter() {
        UiRouter thisOrParentUiRouter = getThisOrParentUiRouter();
        if (thisOrParentUiRouter == null)
            setUiRouter(thisOrParentUiRouter = UiRouter.create(this));
        return thisOrParentUiRouter;
    }

    private UiRouter getThisOrParentUiRouter() {
        if (uiRouter != null)
            return uiRouter;
        ActivityContext parentContext = getParentContext();
        if (parentContext instanceof UiRouteActivityContext)
            return ((UiRouteActivityContext) parentContext).getUiRouter();
        return null;
    }

    @Override
    public BrowsingHistory getHistory() {
        UiRouter thisOrParentUiRouter = getThisOrParentUiRouter();
        if (thisOrParentUiRouter == null)
            return WindowHistory.getProvider();
        return thisOrParentUiRouter.getHistory();
    }

    public void setParams(ReadOnlyAstObject params) {
        this.params = params;
    }

    @Override
    public ReadOnlyAstObject getParams() {
        if (params == null)
            params = AST.createObject();
        return params;
    }

    @Override
    public <T> T getParameter(String key) {
        T value = getParams().get(key);
        if (value == null && !params.has(key)) {
            ActivityContext parentContext = getParentContext();
            if (parentContext instanceof UiRouteActivityContext)
                return ((UiRouteActivityContext<?>) parentContext).getParameter(key);
        }
        return value;
    }

    @Override
    public String getRoutingPath() {
        String routingPath = this.routingPath;
        if (routingPath == null) {
            ActivityContext parentContext = getParentContext();
            if (parentContext instanceof UiRouteActivityContext)
                return ((UiRouteActivityContext<?>) parentContext).getRoutingPath();
        }
        return routingPath;
    }

    public void setRoutingPath(String routingPath) {
        this.routingPath = routingPath;
    }

    public static <IC extends ActivityContext<IC>, OC extends UiRouteActivityContextBase<OC>> OC toUiRouterActivityContextBase(IC activityContext) {
        return from(activityContext, ac -> ac instanceof UiRouteActivityContextBase);
    }

    private final BooleanProperty activeProperty = new SimpleBooleanProperty(false);
    @Override
    public ObservableBooleanValue activeProperty() {
        return activeProperty;
    }

}
