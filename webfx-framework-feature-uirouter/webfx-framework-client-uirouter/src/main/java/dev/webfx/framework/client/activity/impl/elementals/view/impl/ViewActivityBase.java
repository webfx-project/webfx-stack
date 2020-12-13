package dev.webfx.framework.client.activity.impl.elementals.view.impl;

import javafx.scene.Node;
import dev.webfx.kit.launcher.WebFxKitLauncher;
import dev.webfx.platform.shared.util.async.Future;
import dev.webfx.framework.client.activity.impl.elementals.uiroute.impl.UiRouteActivityBase;
import dev.webfx.framework.client.activity.impl.elementals.view.ViewActivity;
import dev.webfx.framework.client.activity.impl.elementals.view.ViewActivityContext;
import dev.webfx.framework.client.activity.impl.elementals.view.ViewActivityContextMixin;

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
            return Future.runAsync(this::onResume);
        Future<Void> future = Future.future();
        WebFxKitLauncher.onReady(() -> {
            onResume();
            future.complete();
        });
        return future;
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
