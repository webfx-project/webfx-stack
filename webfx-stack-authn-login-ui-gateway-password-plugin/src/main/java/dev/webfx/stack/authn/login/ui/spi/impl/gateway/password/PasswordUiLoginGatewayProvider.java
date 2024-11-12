package dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;

import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.util.animation.Animations;
import dev.webfx.extras.util.layout.LayoutUtil;
import dev.webfx.extras.util.scene.SceneUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.windowlocation.WindowLocation;
import dev.webfx.stack.authn.AuthenticationRequest;
import dev.webfx.stack.authn.MagicLinkRequest;
import dev.webfx.stack.authn.UsernamePasswordCredentials;
import dev.webfx.stack.authn.login.ui.FXLoginContext;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProviderBase;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.session.state.client.fx.FXUserId;
import dev.webfx.stack.ui.controls.MaterialFactoryMixin;
import dev.webfx.stack.ui.controls.button.ButtonFactory;
import dev.webfx.stack.ui.controls.dialog.GridPaneBuilder;
import dev.webfx.stack.ui.operation.OperationUtil;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import one.modality.base.client.icons.SvgIcons;

/**
 * @author Bruno Salmon
 */
public final class PasswordUiLoginGatewayProvider extends UiLoginGatewayProviderBase implements MaterialFactoryMixin {

    private final static String GATEWAY_ID = "Password";

    private Label titleLabel;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorMessageLabel;
    private VBox successMessageVBox;
    private double passwordPrefHeight;
    private Button button;
    // SignInMode = true => username/password, false => magic link
    private final Property<Boolean> signInMode = new SimpleObjectProperty<>(true);
    //private final ModalityValidationSupport validationSupport = new ModalityValidationSupport();

    public PasswordUiLoginGatewayProvider() {
        super(GATEWAY_ID);
        FXProperties.runOnPropertiesChange(() -> {
            if (FXUserId.getUserId() != null)
                resetUXToLogin();
        }, FXUserId.userIdProperty());
    }

    @Override
    public Node createLoginButton() {
        return new Text("Password");
    }

    //TODO remplacer les strings en dur avec du I18n

    @Override
    public Node createLoginUi(UiLoginPortalCallback callback) {
        BorderPane loginWindow = new BorderPane();
        Hyperlink hyperLink = newHyperlink(null, e -> signInMode.setValue(!signInMode.getValue()));
        hyperLink.getStyleClass().add(Bootstrap.TEXT_INFO);
        GridPane.setMargin(hyperLink, new Insets(20));
        titleLabel = Bootstrap.textPrimary(I18nControls.newLabel(PasswordI18nKeys.Login));
        titleLabel.getStyleClass().add(Bootstrap.H3);
        // Create the error message label, initially hidden
        errorMessageLabel = Bootstrap.textDanger(I18nControls.newLabel(PasswordI18nKeys.IncorrectLoginOrPassword));
        errorMessageLabel.setVisible(false);
        successMessageVBox = new VBox();
        successMessageVBox.setSpacing(15);
        successMessageVBox.setAlignment(Pos.CENTER);
        successMessageVBox.setVisible(false);

        GridPane gridPane = new GridPaneBuilder()
            .addNodeFillingRow(titleLabel)
            .addNodeFillingRow(successMessageVBox)
            .addNodeFillingRow(usernameField = newMaterialTextField(PasswordI18nKeys.Email))
            .addNodeFillingRow(passwordField = newMaterialPasswordField(PasswordI18nKeys.Password))
            .addNodeFillingRow(errorMessageLabel)
            .addNewRow(hyperLink)
            .addNodeFillingRow(button = Bootstrap.largePrimaryButton(new Button()))
            .build();
        RowConstraints firstRowConstraints = new RowConstraints();
        firstRowConstraints.setMinHeight(Region.USE_PREF_SIZE);
        firstRowConstraints.setVgrow(Priority.SOMETIMES);
        firstRowConstraints.setMinHeight(80);
        gridPane.getRowConstraints().add(firstRowConstraints);

        GridPane.setHalignment(titleLabel, HPos.CENTER);  // Horizontally center
        GridPane.setValignment(titleLabel, VPos.TOP);

        LayoutUtil.setMaxWidthToInfinite(button);
        button.setPadding(new Insets(15));
        LayoutUtil.setPrefWidthToInfinite(gridPane);
        loginWindow.setCenter(gridPane);
        GridPane.setHalignment(hyperLink, HPos.CENTER);
        hyperLink.setOnAction(e -> signInMode.setValue(!signInMode.getValue()));
        FXProperties.runNowAndOnPropertiesChange(() -> {
            boolean signIn = signInMode.getValue();
            I18nControls.bindI18nProperties(titleLabel, signIn ? PasswordI18nKeys.Login : PasswordI18nKeys.Recovery);
            I18nControls.bindI18nProperties(button, signIn ? PasswordI18nKeys.SignIn + ">>" : PasswordI18nKeys.SendLink+">>");
            I18nControls.bindI18nProperties(hyperLink, signIn ? PasswordI18nKeys.ForgotPassword : PasswordI18nKeys.RememberPassword);
            errorMessageLabel.setVisible(false);
            successMessageVBox.setVisible(false);
            passwordField.setVisible(signIn);
            if (!signIn) {
                if (passwordPrefHeight == 0) {
                    passwordPrefHeight = passwordField.getHeight();
                    passwordField.setPrefHeight(passwordPrefHeight);
                }
                passwordField.setMinHeight(0);
            }
            Animations.animateProperty(passwordField.prefHeightProperty(), signIn ? passwordPrefHeight : 0)
                .setOnFinished(e -> passwordField.setMinHeight(signIn ? -1 : 0));
        }, signInMode);
        //initValidation();
        button.setOnAction(event -> {
            //if (validationSupport.isValid())
            errorMessageLabel.setVisible(false);
            successMessageVBox.setVisible(false);
            successMessageVBox.getChildren().clear();
            Object credentials = signInMode.getValue() ?
                new UsernamePasswordCredentials(usernameField.getText(), passwordField.getText())
                : new MagicLinkRequest(usernameField.getText(), WindowLocation.getOrigin(), WindowLocation.getPath(), I18n.getLanguage(), FXLoginContext.getLoginContext());
            OperationUtil.turnOnButtonsWaitMode(button);
            new AuthenticationRequest()
                .setUserCredentials(credentials)
                .executeAsync()
                .onComplete(ar -> UiScheduler.runInUiThread(() -> OperationUtil.turnOffButtonsWaitMode(button)))
                .onFailure(failure-> {
                    callback.notifyUserLoginFailed(failure);
                    errorMessageLabel.setVisible(true);
                })
                .onSuccess(ignored -> UiScheduler.runInUiThread(() -> {
                    if (signInMode.getValue()) {
                        usernameField.clear();
                    }
                    else {
                        //Case of the magic link
                        successMessageVBox.setVisible(true);
                        successMessageVBox.setManaged(true);
                        Label message = Bootstrap.textSuccess(I18nControls.newLabel(PasswordI18nKeys.LinkSent));
                        message.setWrapText(true);
                        ProgressIndicator progressIndicator = new ProgressIndicator();
                        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                        Label waitingMessage = Bootstrap.textSecondary(I18nControls.newLabel("Waiting for the validation of the hyperlink"));
                        waitingMessage.setWrapText(true);
                        successMessageVBox.getChildren().addAll(message,progressIndicator,waitingMessage);
                        button.setVisible(false);
                        usernameField.setVisible(false);
                        //hyperLink.setVisible(false);
                    }
                    passwordField.clear();
                    //callback.notifyUserLoginSuccessful();
                    SVGPath checkMark = SvgIcons.createCheckMarkSVGPath();
                    ScalePane scalePane = new ScalePane(checkMark);
                    button.graphicProperty().unbind();
                    button.setGraphic(scalePane);
                    //button.textProperty().unbind();
                    //button.setText(null);
                }));
        });
        FXProperties.runNowAndOnPropertiesChange(this::prepareShowing, loginWindow.sceneProperty());
        return loginWindow;
    }

    /*private void initValidation() {
        validationSupport.addRequiredInput(usernameField, "Username is required");
        validationSupport.addRequiredInput(passwordField, "Password is required");
    }*/

    private void resetUXToLogin() {
        signInMode.setValue(true);
        button.setVisible(true);
        successMessageVBox.setVisible(false);
        successMessageVBox.setManaged(false);
        usernameField.setVisible(true);
        button.setGraphic(null);
    }

    public void prepareShowing() {
        I18nControls.bindI18nProperties(button, signInMode.getValue() ? "SignIn>>" : "SendLink>>");
        // Resetting the default button (required for JavaFX if displayed a second time)
        ButtonFactory.resetDefaultButton(button);
        SceneUtil.autoFocusIfEnabled(usernameField);
        UiScheduler.scheduleDelay(500, () -> SceneUtil.autoFocusIfEnabled(usernameField));
    }

}
