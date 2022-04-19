package dev.webfx.framework.client.activity.impl.elementals.view.impl;

import dev.webfx.framework.client.activity.impl.elementals.uiroute.impl.UiRouteActivityBase;
import dev.webfx.framework.client.activity.impl.elementals.view.ViewActivity;
import dev.webfx.framework.client.activity.impl.elementals.view.ViewActivityContext;
import dev.webfx.framework.client.activity.impl.elementals.view.ViewActivityContextMixin;
import dev.webfx.kit.launcher.WebFxKitLauncher;
import dev.webfx.platform.shared.async.AsyncUtil;
import dev.webfx.platform.shared.async.Future;
import dev.webfx.platform.shared.async.Promise;
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
