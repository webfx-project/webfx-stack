package dev.webfx.stack.authn.login.ui.spi.impl.gateway.facebook;

import dev.webfx.platform.resource.Resource;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.WebViewBasedUiLoginGatewayProvider;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/**
 * @author Bruno Salmon
 */
public final class FacebookUiLoginGatewayProvider extends WebViewBasedUiLoginGatewayProvider {

    private final static String GATEWAY_ID = "Facebook";

    public FacebookUiLoginGatewayProvider() {
        super(GATEWAY_ID);
    }

    @Override
    public Button createLoginButton() {
        ImageView fLogo = new ImageView(Resource.toUrl("F.png", getClass()));
        fLogo.setFitWidth(24);
        fLogo.setFitHeight(24);
        return createLoginButton(fLogo, Color.WHITE, Color.web("#385399"));
    }

}
