package dev.webfx.stack.authn.login.ui.spi.impl.portal;

import dev.webfx.extras.panes.FlipPane;
import dev.webfx.extras.panes.GoldenRatioPane;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.util.animation.Animations;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.os.OperatingSystem;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProvider;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.magiclink.MagicLinkUi;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
final class LoginPortalUi implements UiLoginPortalCallback {

    private final FlipPane flipPane = new FlipPane();
    private final Region backgroundRegion = new Region();
    private final Region leftLine = new Region();
    private final Text orText = new Text("OR");
    private final Region rightLine = new Region();
    private Node userUI;
    private final List<Node> otherLoginButtons = new ArrayList<>();

    private final GoldenRatioPane loginPaneContainer = new GoldenRatioPane();
    private final Pane loginPane = new Pane(backgroundRegion, leftLine, orText, rightLine) {

        @Override
        protected void layoutChildren() {
            double width = getWidth(), height = getHeight();
            double margin = 40, x = margin, y = margin, w = width - 2 * margin, h, wor = orText.prefWidth(w), wl = w * 0.5 - wor;
            layoutInArea(backgroundRegion, 0, 0, width, height, 0, null, HPos.LEFT, VPos.TOP);
            layoutInArea(userUI, x, y, w, h = Math.min(userUI.prefHeight(w), height - 2 * margin), 0, Insets.EMPTY, false, false, HPos.CENTER, VPos.TOP);
            int n = otherLoginButtons.size();
            boolean hasOtherLoginButtons = n > 0;
            orText.setVisible(hasOtherLoginButtons);
            leftLine.setVisible(hasOtherLoginButtons);
            rightLine.setVisible(hasOtherLoginButtons);
            if (hasOtherLoginButtons) {
                layoutInArea(orText, x, y += h + margin, w, 0, 0, null, false, false, HPos.CENTER, VPos.CENTER);
                layoutInArea(leftLine, x, y, wl, 1, 0, HPos.LEFT, VPos.CENTER);
                layoutInArea(rightLine, x + w -wl, y, wl, 1, 0, HPos.RIGHT, VPos.CENTER);
                double[] prefWidths = new double[n];
                double prefWidthTotal = 24 * (n - 1);
                for (int i = 0; i < n; i++) {
                    prefWidthTotal += prefWidths[i] = otherLoginButtons.get(i).prefWidth(24);
                }
                x = x + w / 2 - prefWidthTotal / 2;
                y += margin;
                h = 24;
                for (int i = 0; i < n; i++) {
                    layoutInArea(otherLoginButtons.get(i), x, y, prefWidths[i], h , 0, Insets.EMPTY, false, true, HPos.CENTER, VPos.CENTER);
                    x += prefWidths[i] + 24;
                }
            }
        }

        {
            setMinHeight(USE_PREF_SIZE);
            setMaxSize(400, Region.USE_PREF_SIZE);
        }

        @Override
        protected double computePrefHeight(double width) {
            double margin = 40, y = margin, w = Math.max(-1 , width - 2 * margin);
            double h = userUI.prefHeight(w); // userPasswordUI
            y += h;
            if (!otherLoginButtons.isEmpty()) {
                y += margin; // orText
            }
            h = 24;
            return y + h + margin;
        }
    };

    public LoginPortalUi(StringProperty magicLinkTokenProperty, Consumer<String> requestedPathConsumer) {
        //If MagicLink, we display only the MagikLink panel
        if (magicLinkTokenProperty != null)
            userUI = new MagicLinkUi(magicLinkTokenProperty, requestedPathConsumer).getUi();
        else {
            for (UiLoginGatewayProvider gatewayProvider : UiLoginPortalProvider.getProviders()) {
                Object gatewayId = gatewayProvider.getGatewayId();
                if ("Password".equals(gatewayId)) {
                    userUI = gatewayProvider.createLoginUi(this);
                } else {
                    //If we have magicklink to true, we do nothing
                    StackPane loginButton = new StackPane(gatewayProvider.createLoginButton());
                    loginButton.setPadding(new Insets(13));
                    loginButton.setPrefSize(50, 50);
                    loginButton.setOnMouseClicked(e -> {
                        Button backButton = new Button("« Use another method to sign in");
                        backButton.setPadding(new Insets(15));
                        BorderPane.setAlignment(backButton, Pos.CENTER);
                        BorderPane.setMargin(backButton, new Insets(10));
                        backButton.setOnAction(e2 -> showLoginHome());
                        BorderPane borderPane = new BorderPane();
                        borderPane.setBottom(backButton);
                        flipPane.setBack(borderPane);
                        Node loginUi = gatewayProvider.createLoginUi(this); // probably a web-view
                        if (!OperatingSystem.isMobile()) {
                            borderPane.setCenter(loginUi);
                            flipPane.flipToBack();
                        } else {
                            // On mobiles, we wait the flip animation to be finished (borderPane in stable position) before
                            // attaching the web-view (otherwise the web view is not visible).
                            flipPane.flipToBack(() -> borderPane.setCenter(loginUi));
                            // Now the "Sign in with Google" button appears, but it is not reacting (no popup)...
                            // 2 possible causes:
                            // 1) Popup is not working on the web-view (or required setup)
                            // 2) Google doesn't allow SSO in web-view (see https://developers.googleblog.com/2016/08/modernizing-oauth-interactions-in-native-apps.html)
                        }
                    });
                    otherLoginButtons.add(loginButton);
                }
            }
        }
        loginPaneContainer.setContent(loginPane);
        loginPane.getChildren().add(userUI);
        loginPane.getChildren().addAll(otherLoginButtons);
        orText.getStyleClass().add("or");
        leftLine.setMinHeight(1);
        leftLine.getStyleClass().add("line");
        rightLine.setMinHeight(1);
        rightLine.getStyleClass().add("line");
        backgroundRegion.getStyleClass().addAll("background", "fx-border");
        FXProperties.runNowAndOnPropertyChange(this::showLoginHome, flipPane.sceneProperty());
        flipPane.getStyleClass().add("login");
        loginPane.getStyleClass().add("login-child");
    }

    public FlipPane getFlipPane() {
        return flipPane;
    }

    void showLoginHome() {
        flipPane.setFront(loginPaneContainer);
        if (flipPane.getScene() != null)
            flipPane.flipToFront();
    }

    // Callback

    @Override
    public void notifyInitializationFailure() {
        Platform.runLater(() -> {
            SVGPath errorLogo = new SVGPath();
            errorLogo.setContent("M 16 7 C 13.36052 7 11.067005 8.2378107 9.421875 10.052734 C 9.2797018 10.03501 9.1552693 10 9 10 C 6.8026661 10 5 11.802666 5 14 C 5 14.0074 5.0018931 14.008395 5.0019531 14.015625 C 3.2697139 15.069795 2 16.832921 2 19 C 2 22.301625 4.6983746 25 8 25 L 24 25 C 27.301625 25 30 22.301625 30 19 C 30 15.842259 27.509898 13.303165 24.40625 13.082031 C 23.18074 9.5665933 19.923127 7 16 7 z M 16 9 C 19.27847 9 22.005734 11.243586 22.775391 14.271484 L 22.978516 15.072266 L 23.800781 15.023438 C 24.012411 15.011276 24.071091 15 24 15 C 26.220375 15 28 16.779625 28 19 C 28 21.220375 26.220375 23 24 23 L 8 23 C 5.7796254 23 4 21.220375 4 19 C 4 17.338324 5.0052754 15.930166 6.4335938 15.320312 L 7.1289062 15.023438 L 7.03125 14.271484 C 7.0103607 14.109285 7 14.025078 7 14 C 7 12.883334 7.8833339 12 9 12 C 9.14 12 9.2894098 12.02145 9.4628906 12.0625 L 10.087891 12.208984 L 10.482422 11.703125 C 11.765559 10.05801 13.75001 9 16 9 z M 15.984375 10.986328 A 1.0001 1.0001 0 0 0 15 12 L 15 16 A 1.0001 1.0001 0 1 0 17 16 L 17 12 A 1.0001 1.0001 0 0 0 15.984375 10.986328 z M 16 19 A 1 1 0 0 0 15 20 A 1 1 0 0 0 16 21 A 1 1 0 0 0 17 20 A 1 1 0 0 0 16 19 z");
            Color errorColor = Color.web("#D8403A");
            errorLogo.setFill(errorColor);
            ScalePane errorLogoScalePane = new ScalePane(errorLogo);
            errorLogoScalePane.setPrefHeight(150);
            Text errorText = new Text("This service is currently unavailable.");
            errorText.setFill(errorColor);
            Text tryAgainText = new Text("Please try again later.");
            tryAgainText.setFill(errorColor);
            VBox vBox = new VBox(10, errorLogoScalePane, errorText, tryAgainText);
            vBox.setAlignment(Pos.CENTER);
            vBox.setMaxWidth(400);
            vBox.getStyleClass().add("error");
            Node back = flipPane.getBack();
            if (back instanceof BorderPane) {
                ((BorderPane) back).setCenter(vBox);
            } else
                flipPane.setBack(vBox);
            flipPane.flipToBack();
            UiScheduler.scheduleDelay(2500, this::showLoginHome);
        });
    }

    @Override
    public void notifyUserLoginSuccessful() {
        Platform.runLater(() -> {
            SVGPath success = new SVGPath();
            success.setContent("M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z M14.7 8.39l-3.78 5-1.63-2.11a1 1 0 0 0-1.58 1.23l2.43 3.11a1 1 0 0 0 .79.38 1 1 0 0 0 .79-.39l4.57-6a1 1 0 1 0-1.6-1.22z");
            success.setFill(Color.web("#0096D6FF"));
            ScalePane scalePane = new ScalePane(success);
            scalePane.setMaxWidth(250);
            scalePane.setMinHeight(16);
            flipPane.setBack(scalePane);
            flipPane.flipToBack();
        });
    }

    @Override
    public void notifyUserLoginFailed(Throwable cause) {
        Animations.shake(flipPane.getFront());
        Console.log("Authentication failed", cause);
    }

}

