package dev.webfx.stack.auth.login.ui;

import dev.webfx.platform.util.serviceloader.SingleServiceProvider;
import dev.webfx.stack.auth.login.ui.spi.LoginUiProvider;
import javafx.scene.Node;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class LoginUi {

    public static LoginUiProvider getProvider() {
        return SingleServiceProvider.getProvider(LoginUiProvider.class, () -> ServiceLoader.load(LoginUiProvider.class));
    }

    public static Node createLoginUI() {
        return getProvider().createLoginUi();
    }

}
