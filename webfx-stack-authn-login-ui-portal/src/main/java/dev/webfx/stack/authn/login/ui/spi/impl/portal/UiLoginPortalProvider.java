package dev.webfx.stack.authn.login.ui.spi.impl.portal;

import dev.webfx.platform.service.MultipleServiceProviders;
import dev.webfx.stack.authn.login.ui.spi.UiLoginServiceProvider;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import javafx.scene.Node;

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
    public Node createLoginUi() {
        LoginPortalUi loginPortalUi = getHiddenLoginPortalUi();
        if (loginPortalUi == null) {
            loginPortalUis.add(loginPortalUi = new LoginPortalUi());
        }
        return loginPortalUi.getFlipPane();
    }

    private LoginPortalUi getHiddenLoginPortalUi() {
        for (LoginPortalUi loginPortalUi : loginPortalUis) {
            if (loginPortalUi.getFlipPane().getScene() == null)
                return loginPortalUi;
        }
        return null;
    }

    private LoginPortalUi getDisplayedLoginPortalUi() {
        for (LoginPortalUi loginPortalUi : loginPortalUis) {
            if (loginPortalUi.getFlipPane().getScene() != null)
                return loginPortalUi;
        }
        return null;
    }

    // Callbacks

    @Override
    public void notifyInitializationFailure() {
        LoginPortalUi loginPortalUi = getDisplayedLoginPortalUi();
        if (loginPortalUi != null) {
            loginPortalUi.notifyInitializationFailure();
        }
    }

    @Override
    public void notifyUserLoginSuccessful() {
        LoginPortalUi loginPortalUi = getDisplayedLoginPortalUi();
        if (loginPortalUi != null) {
            loginPortalUi.notifyUserLoginSuccessful();
        }
    }

    @Override
    public void notifyUserLoginFailed(Throwable cause) {
        LoginPortalUi loginPortalUi = getDisplayedLoginPortalUi();
        if (loginPortalUi != null) {
            loginPortalUi.notifyUserLoginFailed(cause);
        }
    }

}
