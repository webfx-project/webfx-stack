package dev.webfx.stack.authn.login.ui.spi.impl.gateway.facebook;

import dev.webfx.platform.resource.Resource;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.WebViewBasedUiLoginGatewayProvider;
import javafx.scene.Node;
import javafx.scene.image.ImageView;

/**
 * @author Bruno Salmon
 */
public final class FacebookUiLoginGatewayProvider extends WebViewBasedUiLoginGatewayProvider {

    private final static String GATEWAY_ID = "Facebook";

    public FacebookUiLoginGatewayProvider() {
        super(GATEWAY_ID);
    }

    @Override
    public Node createLoginButton() {
        ImageView fLogo = new ImageView(Resource.toUrl("F.png", getClass()));
        fLogo.setFitWidth(24);
        fLogo.setFitHeight(24);
        return fLogo;
    }

}
