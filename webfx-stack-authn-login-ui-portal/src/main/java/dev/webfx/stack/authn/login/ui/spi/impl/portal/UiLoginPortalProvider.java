package dev.webfx.stack.authn.login.ui.spi.impl.portal;

import dev.webfx.extras.panes.FlipPane;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.service.MultipleServiceProviders;
import dev.webfx.stack.authn.login.ui.spi.UiLoginServiceProvider;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public class UiLoginPortalProvider implements UiLoginServiceProvider, UiLoginPortalCallback {

    static List<UiLoginGatewayProvider> getProviders() {
        return MultipleServiceProviders.getProviders(UiLoginGatewayProvider.class, () -> ServiceLoader.load(UiLoginGatewayProvider.class));
    }

    private final List<LoginPortalUi> loginPortalUis = new ArrayList<>();

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
        return getLoginPortalUi(false);
    }

    private LoginPortalUi getAttachedLoginPortalUi() {
        return getLoginPortalUi(true);
    }

    private LoginPortalUi getLoginPortalUi(boolean attached) {
        for (LoginPortalUi loginPortalUi : loginPortalUis) {
            if ((loginPortalUi.getFlipPane().getScene() != null) == attached)
                return loginPortalUi;
        }
        return null;
    }

    private LoginPortalUi getActiveLoginPortalUi() {
        return getAttachedLoginPortalUi();
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
