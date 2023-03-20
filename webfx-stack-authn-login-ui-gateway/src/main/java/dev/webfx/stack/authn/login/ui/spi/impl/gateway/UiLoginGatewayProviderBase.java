package dev.webfx.stack.authn.login.ui.spi.impl.gateway;

import dev.webfx.extras.util.background.BackgroundFactory;
import dev.webfx.extras.util.border.BorderFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * @author Bruno Salmon
 */
public abstract class UiLoginGatewayProviderBase implements UiLoginGatewayProvider {

    private final Object gatewayId;

    public UiLoginGatewayProviderBase(Object gatewayId) {
        this.gatewayId = gatewayId;
    }

    @Override
    public Object getGatewayId() {
        return gatewayId;
    }

    public Button createLoginButton() {
        return createLoginButton(null, Color.BLACK, Color.WHITE);
    }
    protected Button createLoginButton(Node graphic, Color textFill, Color backgroundColor) {
        return createLoginButton(graphic, "" + getGatewayId(), textFill, backgroundColor);
    }

    protected Button createLoginButton(Node graphic, String text, Color textFill, Color backgroundColor) {
        Button button = new Button(text, graphic);
        button.setGraphicTextGap(10);
        button.setPadding(new Insets(20));
        button.setTextFill(textFill);
        button.setFont(getFont());
        button.setMaxHeight(80);
        int radius = 5;
        button.setBorder(BorderFactory.newBorder(Color.BLACK, radius));
        button.setBackground(BackgroundFactory.newBackground(backgroundColor, radius));
        return button;
    }

    protected Font getFont() {
        return Font.font(getFontSize());
    }

    protected double getFontSize() {
        return 24;
    }

}
