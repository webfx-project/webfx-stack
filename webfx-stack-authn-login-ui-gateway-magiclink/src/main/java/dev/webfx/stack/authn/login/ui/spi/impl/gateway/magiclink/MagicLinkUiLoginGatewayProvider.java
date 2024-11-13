package dev.webfx.stack.authn.login.ui.spi.impl.gateway.magiclink;

import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProviderBase;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import dev.webfx.stack.ui.controls.MaterialFactoryMixin;
import javafx.scene.Node;

/**
 * @author Bruno Salmon
 */
public class MagicLinkUiLoginGatewayProvider extends UiLoginGatewayProviderBase implements MaterialFactoryMixin {

    private final static String GATEWAY_ID = "MagicLink";

    public MagicLinkUiLoginGatewayProvider() {
        super(GATEWAY_ID);
    }

    @Override
    public Node createLoginButton() {
        return null;
    }

    @Override
    public Node createLoginUi(UiLoginPortalCallback callback) {
        return null;
    }
}
