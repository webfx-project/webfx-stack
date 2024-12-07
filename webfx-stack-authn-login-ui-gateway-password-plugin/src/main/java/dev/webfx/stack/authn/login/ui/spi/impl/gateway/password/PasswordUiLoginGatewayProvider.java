package dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;

import dev.webfx.extras.styles.bootstrap.Bootstrap;
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
import dev.webfx.stack.ui.operation.OperationUtil;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.text.Text;

import java.util.function.Supplier;

/**
 * @author Bruno Salmon
 */
public final class PasswordUiLoginGatewayProvider extends UiLoginGatewayProviderBase implements MaterialFactoryMixin {

   private final static String GATEWAY_ID = "Password";

   private static Supplier<Node> CREATE_ACCOUNT_UI_SUPPLIER;

   private UILoginView uiLoginView;

    // SignInMode = true => username/password, false => magic link
    private final Property<Boolean> signInModeProperty = new SimpleObjectProperty<>(true);
    //private final ModalityValidationSupport validationSupport = new ModalityValidationSupport();


    public static void setCreateAccountUiSupplier(Supplier<Node> createAccountUiSupplier) {
        CREATE_ACCOUNT_UI_SUPPLIER = createAccountUiSupplier;
    }

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

    @Override
    public Node createLoginUi(UiLoginPortalCallback callback) {
        uiLoginView = new UILoginView();
        uiLoginView.initializeComponents();

        uiLoginView.getHyperlink().setOnAction(e -> signInModeProperty.setValue(!signInModeProperty.getValue()));
        uiLoginView.getHyperlink().getStyleClass().add(Bootstrap.TEXT_INFO);

        FXProperties.runNowAndOnPropertiesChange(() -> {
            boolean signIn = signInModeProperty.getValue();
            I18nControls.bindI18nProperties(uiLoginView.getActionButton(), signIn ? PasswordI18nKeys.Login : PasswordI18nKeys.SendLink+">>");
            I18nControls.bindI18nProperties(uiLoginView.getHyperlink(), signIn ? PasswordI18nKeys.ForgotPassword : PasswordI18nKeys.RememberPassword);
            I18nControls.bindI18nProperties(uiLoginView.getLoginTitleLabel(), signIn ? PasswordI18nKeys.Login : PasswordI18nKeys.Recovery);
            uiLoginView.hideGraphicFromActionButton();
            uiLoginView.hideMessageForPasswordField();
            if (!signIn) {
                uiLoginView.hidePasswordField();
            }
            else {
                uiLoginView.showPasswordField();
            }
        }, signInModeProperty);
        uiLoginView.getActionButton().setOnAction(event -> {
            Object credentials = signInModeProperty.getValue() ?
                new UsernamePasswordCredentials(uiLoginView.getEmailTextField().getText(), uiLoginView.getPasswordField().getText())
                : new MagicLinkRequest(uiLoginView.getEmailTextField().getText(), WindowLocation.getOrigin(), WindowLocation.getPath(), I18n.getLanguage(), FXLoginContext.getLoginContext());
            OperationUtil.turnOnButtonsWaitMode(uiLoginView.getActionButton());
            new AuthenticationRequest()
                .setUserCredentials(credentials)
                .executeAsync()
                .onComplete(ar -> UiScheduler.runInUiThread(() -> OperationUtil.turnOffButtonsWaitMode(uiLoginView.getActionButton())))
                .onFailure(failure-> {
                    callback.notifyUserLoginFailed(failure);
                    Platform.runLater(()-> {
                        uiLoginView.setInfoMessageForPasswordFieldLabel(PasswordI18nKeys.IncorrectLoginOrPassword,Bootstrap.TEXT_DANGER);
                        uiLoginView.showMessageForPasswordField();
                    });
                })
                .onSuccess(ignored -> UiScheduler.runInUiThread(() -> {
                    if (signInModeProperty.getValue()) {
                        uiLoginView.getEmailTextField().clear();
                        uiLoginView.getPasswordField().clear();
                        uiLoginView.hideGraphicFromActionButton();
                    }
                    else {
                        //Case of the magic link
                        uiLoginView.setTitle(PasswordI18nKeys.Recovery);
                        uiLoginView.setMainMessage(PasswordI18nKeys.LinkSent,Bootstrap.TEXT_SUCCESS);
                        uiLoginView.showMainMessage();
                        uiLoginView.getActionButton().setDisable(true);
                        uiLoginView.getEmailTextField().setDisable(true);
                        uiLoginView.hidePasswordField();
                        uiLoginView.hideHyperlink();
                        uiLoginView.showGraphicFromActionButton();
                    }
                }));
        });
        FXProperties.runNowAndOnPropertiesChange(this::prepareShowing, uiLoginView.getContainer().sceneProperty());
        return uiLoginView.getContainer();
    }

    /*private void initValidation() {
        validationSupport.addRequiredInput(usernameField, "Username is required");
        validationSupport.addRequiredInput(passwordField, "Password is required");
    }*/

    private void resetUXToLogin() {
        signInModeProperty.setValue(true);
        uiLoginView.hideMainMessage();
        uiLoginView.setInfoMessageForPasswordFieldLabel("",Bootstrap.TEXT_INFO);
        uiLoginView.hideGraphicFromActionButton();
        uiLoginView.showEmailField();
        uiLoginView.showPasswordField();
        uiLoginView.hideGraphicFromActionButton();
    }

    public void prepareShowing() {
        I18nControls.bindI18nProperties(uiLoginView.getActionButton(), signInModeProperty.getValue() ? "SignIn>>" : "SendLink>>");
        // Resetting the default button (required for JavaFX if displayed a second time)
        ButtonFactory.resetDefaultButton(uiLoginView.getActionButton());
        SceneUtil.autoFocusIfEnabled(uiLoginView.getEmailTextField());
        UiScheduler.scheduleDelay(500, () -> SceneUtil.autoFocusIfEnabled(uiLoginView.getEmailTextField()));
    }
}
