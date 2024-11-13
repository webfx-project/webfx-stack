package dev.webfx.stack.authn.login.ui;

import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.authn.login.ui.spi.UiLoginServiceProvider;
import javafx.scene.Node;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class LoginUiService {

    public static UiLoginServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(UiLoginServiceProvider.class, () -> ServiceLoader.load(UiLoginServiceProvider.class));
    }

    public static Node createLoginUI() {
        return getProvider().createLoginUi();
    }

    public static Node createMagicLinkUi() {
        return getProvider().createMagicLinkUi();
    }

}
