package dev.webfx.stack.authn.login.ui.spi.impl.portal;

import dev.webfx.platform.service.MultipleServiceProviders;
import dev.webfx.stack.authn.login.ui.spi.UiLoginServiceProvider;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public class UiLoginPortalProvider implements UiLoginServiceProvider, UiLoginPortalCallback {

    static List<UiLoginGatewayProvider> getProviders() {
        return MultipleServiceProviders.getProviders(UiLoginGatewayProvider.class, () -> ServiceLoader.load(UiLoginGatewayProvider.class));
    }

    // Several login ui may be instantiated in different activities, but only one should be visible at a time (= active login)
    private final List<LoginPortalUi> loginPortalUis = new ArrayList<>();

    @Override
    public Node createLoginUi() { // Called each time a login window is required
        return createLoginPortalUi(null, null);
    }

    @Override
    public Node createMagicLinkUi(StringProperty magicLinkTokenProperty, Consumer<String> requestedPathConsumer) { // Called each time a magic link window is required
        return createLoginPortalUi(magicLinkTokenProperty, requestedPathConsumer);
    }

    private Node createLoginPortalUi(StringProperty magicLinkTokenProperty, Consumer<String> requestedPathConsumer) {
        LoginPortalUi loginPortalUi = new LoginPortalUi(magicLinkTokenProperty, requestedPathConsumer);
        loginPortalUis.add(loginPortalUi);
        return loginPortalUi.getFlipPane();
    }

    /* Commented this version which recycles detached login uis, but it still has some issues
    @Override
    public Node createLoginUi() { // Called each time a login window is required
        LoginPortalUi loginPortalUi = getDetachedLoginPortalUi();
        if (loginPortalUi == null) {
            loginPortalUis.add(loginPortalUi = new LoginPortalUi());
        } else {
            // Recycling existing login by resetting it to initial state
            loginPortalUi.showLoginHome();
            FlipPane flipPane = loginPortalUi.getFlipPane();
            FXProperties.setEvenIfBound(flipPane.minHeightProperty(), Region.USE_PREF_SIZE);
        }
        return loginPortalUi.getFlipPane();
    }

    private LoginPortalUi getDetachedLoginPortalUi() {
        for (LoginPortalUi loginPortalUi : loginPortalUis) {
            if (loginPortalUi.getFlipPane().getScene() == null)
                return loginPortalUi;
        }
        return null;
    }
*/

    private LoginPortalUi getActiveLoginPortalUi() {
        for (LoginPortalUi loginPortalUi : loginPortalUis) {
            if (isNodeAndAncestorVisible(loginPortalUi.getFlipPane()))
                return loginPortalUi;
        }
        return null;
    }

    private static boolean isNodeAndAncestorVisible(Node node) {
        if (node == null || !node.isVisible())
            return false;
        Parent parent = node.getParent();
        if (parent != null)
            return isNodeAndAncestorVisible(parent);
        Scene scene = node.getScene();
        return scene != null && node == scene.getRoot();
    }

    // Callbacks

    @Override
    public void notifyInitializationFailure() {
        LoginPortalUi loginPortalUi = getActiveLoginPortalUi();
        if (loginPortalUi != null) {
            loginPortalUi.notifyInitializationFailure();
        }
    }

    @Override
    public void notifyUserLoginSuccessful() {
        LoginPortalUi loginPortalUi = getActiveLoginPortalUi();
        if (loginPortalUi != null) {
            loginPortalUi.notifyUserLoginSuccessful();
        }
    }

    @Override
    public void notifyUserLoginFailed(Throwable cause) {
        LoginPortalUi loginPortalUi = getActiveLoginPortalUi();
        if (loginPortalUi != null) {
            loginPortalUi.notifyUserLoginFailed(cause);
        }
    }

}
