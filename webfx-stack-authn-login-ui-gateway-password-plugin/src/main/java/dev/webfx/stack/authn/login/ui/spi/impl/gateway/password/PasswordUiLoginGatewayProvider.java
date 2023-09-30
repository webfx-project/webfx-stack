package dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;

import dev.webfx.extras.scalepane.ScalePane;
import dev.webfx.extras.util.animation.Animations;
import dev.webfx.extras.util.layout.LayoutUtil;
import dev.webfx.extras.util.scene.SceneUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.AuthenticationRequest;
import dev.webfx.stack.authn.UsernamePasswordCredentials;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProviderBase;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.ui.controls.MaterialFactoryMixin;
import dev.webfx.stack.ui.controls.button.ButtonFactory;
import dev.webfx.stack.ui.controls.dialog.GridPaneBuilder;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

/**
 * @author Bruno Salmon
 */
public final class PasswordUiLoginGatewayProvider extends UiLoginGatewayProviderBase implements MaterialFactoryMixin {

    private final static String GATEWAY_ID = "Password";

    private TextField usernameField;
    private PasswordField passwordField;
    private double passwordPrefHeight;
    private Button button;
    private final Property<Boolean> signInMode = new SimpleObjectProperty<>(true);
    //private final ModalityValidationSupport validationSupport = new ModalityValidationSupport();

    public PasswordUiLoginGatewayProvider() {
        super(GATEWAY_ID);
    }

    @Override
    public Node createLoginButton() {
        return new Text("Password");
    }

    @Override
    public Node createLoginUi(UiLoginPortalCallback callback) {
        BorderPane loginWindow = new BorderPane(); // SectionPanelFactory.createSectionPanel("SignInWindowTitle");
        Hyperlink hyperLink = newHyperlink("ForgotPassword?", e -> signInMode.setValue(!signInMode.getValue()));
        GridPane.setMargin(hyperLink, new Insets(20));
        GridPane gridPane = new GridPaneBuilder()
                .addNodeFillingRow(usernameField = newMaterialTextField("Email"))
                .addNodeFillingRow(passwordField = newMaterialPasswordField("Password"))
                .addNewRow(hyperLink)
                .addNodeFillingRow(button = new Button())
                .build();
        LayoutUtil.setMaxWidthToInfinite(button);
        /* Temporary hard-coded style for the web version */
        button.setPadding(new Insets(15));
        button.setBackground(new Background(new BackgroundFill(Color.web("#0096D6FF"), new CornerRadii(10), null)));
        button.setBorder(null);
        button.setTextFill(Color.WHITE);
        LayoutUtil.setPrefWidthToInfinite(gridPane);
        loginWindow.setCenter(gridPane);
        GridPane.setHalignment(hyperLink, HPos.CENTER);
        hyperLink.setOnAction(e -> signInMode.setValue(!signInMode.getValue()));
        FXProperties.runNowAndOnPropertiesChange(() -> {
            boolean signIn = signInMode.getValue();
            I18nControls.bindI18nProperties(button, signIn ? "SignIn>>" : "SendLink>>");
            I18nControls.bindI18nProperties(hyperLink, signIn ? "ForgotPassword?" : "RememberPassword?");
            passwordField.setVisible(signIn);
            if (!signIn) {
                if (passwordPrefHeight == 0) {
                    passwordPrefHeight = passwordField.getHeight();
                    passwordField.setPrefHeight(passwordPrefHeight);
                }
                passwordField.setMinHeight(0);
            }
            Timeline timeline = Animations.animateProperty(passwordField.prefHeightProperty(), signIn ? passwordPrefHeight : 0);
            if (timeline != null)
                timeline.setOnFinished(e -> passwordField.setMinHeight(signIn ? -1 : 0));
        }, signInMode);
        //initValidation();
        button.setOnAction(event -> {
            //if (validationSupport.isValid())
                new AuthenticationRequest()
                        .setUserCredentials(new UsernamePasswordCredentials(usernameField.getText(), passwordField.getText()))
                        .executeAsync()
                        .onFailure(cause -> callback.notifyUserLoginFailed())
                        .onSuccess(ignored -> Platform.runLater(() -> {
                            usernameField.clear();
                            passwordField.clear();
                            //callback.notifyUserLoginSuccessful();
                            SVGPath checkMark = new SVGPath();
                            checkMark.setContent("M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z M14.7 8.39l-3.78 5-1.63-2.11a1 1 0 0 0-1.58 1.23l2.43 3.11a1 1 0 0 0 .79.38 1 1 0 0 0 .79-.39l4.57-6a1 1 0 1 0-1.6-1.22z");
                            checkMark.setFill(Color.WHITE);
                            ScalePane scalePane = new ScalePane(checkMark);
                            button.graphicProperty().unbind();
                            button.setGraphic(scalePane);
                            button.textProperty().unbind();
                            button.setText(null);
                        }));
        });
        FXProperties.runNowAndOnPropertiesChange(this::prepareShowing, loginWindow.sceneProperty());
        return loginWindow;
    }

    /*private void initValidation() {
        validationSupport.addRequiredInput(usernameField, "Username is required");
        validationSupport.addRequiredInput(passwordField, "Password is required");
    }*/

    public void prepareShowing() {
        I18nControls.bindI18nProperties(button, signInMode.getValue() ? "SignIn>>" : "SendLink>>");
        // Resetting the default button (required for JavaFX if displayed a second time)
        ButtonFactory.resetDefaultButton(button);
        SceneUtil.autoFocusIfEnabled(usernameField);
        UiScheduler.scheduleDelay(500, () -> SceneUtil.autoFocusIfEnabled(usernameField));
    }

}
