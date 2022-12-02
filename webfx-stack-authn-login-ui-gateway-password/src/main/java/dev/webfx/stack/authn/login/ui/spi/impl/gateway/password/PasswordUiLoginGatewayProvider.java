package dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.stack.authn.AuthenticationRequest;
import dev.webfx.stack.authn.UsernamePasswordCredentials;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProviderBase;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.ui.controls.MaterialFactoryMixin;
import dev.webfx.stack.ui.controls.button.ButtonFactory;
import dev.webfx.stack.ui.controls.dialog.GridPaneBuilder;
import dev.webfx.stack.ui.util.anim.Animations;
import dev.webfx.stack.ui.util.layout.LayoutUtil;
import dev.webfx.stack.ui.util.scene.SceneUtil;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

/**
 * @author Bruno Salmon
 */
public final class PasswordUiLoginGatewayProvider extends UiLoginGatewayProviderBase implements MaterialFactoryMixin {

    private final static String GATEWAY_ID = "Password";

    private TextField usernameField;
    private PasswordField passwordField;
    private Button button;
    private final Property<Boolean> signInMode = new SimpleObjectProperty<>(true);
    //private final ModalityValidationSupport validationSupport = new ModalityValidationSupport();

    public PasswordUiLoginGatewayProvider() {
        super(GATEWAY_ID);
    }

    @Override
    public Button createLoginButton() {
        return super.createLoginButton();
    }

    @Override
    public Node createLoginUi() {
        BorderPane loginWindow = new BorderPane(); // SectionPanelFactory.createSectionPanel("SignInWindowTitle");
        Hyperlink hyperLink = newHyperlink("ForgotPassword?", e -> signInMode.setValue(!signInMode.getValue()));
        GridPane gridPane = new GridPaneBuilder()
                .addNodeFillingRow(usernameField = newMaterialTextField("Email"))
                .addNodeFillingRow(passwordField = newMaterialPasswordField("Password"))
                .addNewRow(hyperLink)
                .addNodeFillingRow(button = new Button()/* newLargeGreenButton(null)*/)
                .build();
        loginWindow.setCenter(gridPane);
        gridPane.setPadding(new Insets(20));
        GridPane.setHalignment(hyperLink, HPos.CENTER);
        hyperLink.setOnAction(e -> signInMode.setValue(!signInMode.getValue()));
        LayoutUtil.setUnmanagedWhenInvisible(passwordField, signInMode);
        FXProperties.runNowAndOnPropertiesChange(() ->
                        I18n.bindI18nProperties(button, signInMode.getValue() ? "SignIn>>" : "SendPassword>>")
                , signInMode);
        //initValidation();
        button.setOnAction(event -> {
            dev.webfx.platform.console.Console.log("Executing authentication request");
            //if (validationSupport.isValid())
                new AuthenticationRequest()
                        .setUserCredentials(new UsernamePasswordCredentials(usernameField.getText(), passwordField.getText()))
                        .executeAsync()
                        .onFailure(cause -> {
                            Animations.shake(loginWindow);
                            cause.printStackTrace();
                        })
                        .onSuccess(ignored -> passwordField.clear());
        });
        prepareShowing();
        return LayoutUtil.createGoldLayout(loginWindow);
    }

    /*private void initValidation() {
        validationSupport.addRequiredInput(usernameField, "Username is required");
        validationSupport.addRequiredInput(passwordField, "Password is required");
    }*/

    public void prepareShowing() {
        // Resetting the default button (required for JavaFX if displayed a second time)
        ButtonFactory.resetDefaultButton(button);
        SceneUtil.autoFocusIfEnabled(usernameField);
    }

}
