package dev.webfx.stack.authn.login.ui.spi.impl.gateway.google;

import dev.webfx.platform.resource.Resource;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.WebViewBasedUiLoginGatewayProvider;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * @author Bruno Salmon
 */
public final class GoogleUiLoginGatewayProvider extends WebViewBasedUiLoginGatewayProvider {

    private final static String GATEWAY_ID = "Google";

    private final static Color googleBlue = Color.web("#3A7FFB");
    private final static Color googleRed = Color.web("#EF3F22");
    private final static Color googleOrange = Color.web("#FFBE01");
    private final static Color googleGreen = Color.web("#24A84A");

    public GoogleUiLoginGatewayProvider() {
        super(GATEWAY_ID);
    }

    @Override
    public Button createLoginButton() {
        /*Pane gLogo = new StackPane(
                createSVGRegion(googleRed, "M 12.255 4.75 C 14.025 4.75 15.605 5.36 16.855 6.55 L 20.275 3.13 C 18.205 1.19 15.495 0 12.255 0 C 7.565 0 3.515 2.7 1.545 6.62 L 5.525 9.71 C 6.475 6.86 9.125 4.75 12.255 4.75 Z"),
                createSVGRegion(googleOrange, "M 5.525 14.29 C 5.275 13.57 5.145 12.8 5.145 12 C 5.145 11.2 5.285 10.43 5.525 9.71 L 5.525 6.62 L 1.545 6.62 C 0.725 8.24 0.255 10.06 0.255 12 C 0.255 13.94 0.725 15.76 1.545 17.38 L 5.525 14.29 Z"),
                createSVGRegion(googleGreen, "M 12.255 24 C 15.495 24 18.205 22.92 20.185 21.09 L 16.325 18.09 C 15.245 18.81 13.875 19.25 12.255 19.25 C 9.125 19.25 6.475 17.14 5.525 14.29 L 1.545 14.29 L 1.545 17.38 C 3.515 21.3 7.565 24 12.255 24 Z"),
                createSVGRegion(googleBlue, "M 23.745 12.27 C 23.745 11.48 23.675 10.73 23.555 10 L 12.255 10 L 12.255 14.51 L 18.725 14.51 C 18.435 15.99 17.585 17.24 16.325 18.09 L 16.325 21.09 L 20.185 21.09 C 22.445 19 23.745 15.92 23.745 12.27 Z")
        );*/
        ImageView gLogo = new ImageView(Resource.toUrl("G.png", getClass()));
        gLogo.setFitWidth(24);
        gLogo.setFitHeight(24);
        Color[] googleLetterColors = {googleBlue, googleRed, googleOrange, googleBlue, googleGreen, googleRed};
        HBox googleWord = new HBox(1, gLogo);
        googleWord.setAlignment(Pos.CENTER);
        Font font = getFont();
        for (int i = 1; i < googleLetterColors.length; i++) {
            Text googleLetter = new Text("" + GATEWAY_ID.charAt(i));
            googleLetter.setFont(font);
            googleLetter.setFill(googleLetterColors[i]);
            googleWord.getChildren().add(googleLetter);
        }
        return createLoginButton(googleWord, null, Color.BLACK, Color.WHITE);
    }

    @Override
    protected double getFontSize() {
        return 1.2 * super.getFontSize(); // Not sure why, but the font appears smaller in Text, so applying a scaling factor
    }

    /*private static Region createSVGRegion(Color fill, String content) {
        Region region = new Region();
        // methods not supported by WebFX
        region.setShape(createSVGPath(fill, content));
        region.setScaleShape(false);
        region.setCenterShape(false);
        String style = "-fx-background-color: rgb(" + 255 * fill.getRed() + "," + 255 * fill.getGreen() + "," + 255 * fill.getBlue() + ")";
        System.out.println(style);
        region.setStyle(style);
        LayoutUtil.setMinSize(region, 24);
        LayoutUtil.setPrefSize(region, 24);
        LayoutUtil.setMaxSize(region, 24);
        return region;
    }

    private static SVGPath createSVGPath(Color fill, String content) {
        SVGPath path = new SVGPath();
        path.setFill(fill);
        path.setContent(content);
        return path;
    }*/

}
