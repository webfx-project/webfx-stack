package dev.webfx.stack.authn.login.ui.spi.impl.gateway;

import javafx.scene.Node;
import javafx.scene.control.Button;

public interface UiLoginGatewayProvider {

    Object getGatewayId();

    Button createLoginButton();

    Node createLoginUi();

}
