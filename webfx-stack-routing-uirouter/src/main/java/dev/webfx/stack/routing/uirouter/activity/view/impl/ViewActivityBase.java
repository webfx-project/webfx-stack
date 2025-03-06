package dev.webfx.stack.routing.uirouter.activity.view.impl;

import dev.webfx.stack.routing.uirouter.activity.uiroute.impl.UiRouteActivityBase;
import dev.webfx.stack.routing.uirouter.activity.view.ViewActivity;
import dev.webfx.stack.routing.uirouter.activity.view.ViewActivityContext;
import dev.webfx.stack.routing.uirouter.activity.view.ViewActivityContextMixin;
import dev.webfx.kit.launcher.WebFxKitLauncher;
import dev.webfx.platform.async.util.AsyncUtil;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import javafx.scene.Node;

/**
 * @author Bruno Salmon
 */
public abstract class ViewActivityBase
        <C extends ViewActivityContext<C>>

        extends UiRouteActivityBase<C>
        implements ViewActivity<C>,
        ViewActivityContextMixin<C> {

    protected Node uiNode;

    @Override
    public Future<Void> onResumeAsync() {
        if (WebFxKitLauncher.isReady())
            return AsyncUtil.runAsync(this::onResume);
        Promise<Void> promise = Promise.promise();
        WebFxKitLauncher.onReady(() -> {
            onResume();
            promise.complete();
        });
        return promise.future();
    }

    @Override
    public void onResume() {
        super.onResume(); // will update context parameters from route and make the active property to true
        if (uiNode == null) {
            startLogic(); // The good place to start the logic (before building ui but after the above update)
            uiNode = buildUi();
        }
        setNode(uiNode);
    }

    protected void startLogic() {
    }
}
