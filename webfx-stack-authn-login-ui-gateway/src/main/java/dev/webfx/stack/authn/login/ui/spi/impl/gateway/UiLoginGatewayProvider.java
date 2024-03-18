package dev.webfx.stack.authn.login.ui.spi.impl.gateway;

import javafx.scene.Node;

public interface UiLoginGatewayProvider {

    Object getGatewayId();

    Node createLoginButton();

    Node createLoginUi(UiLoginPortalCallback callback);

}
