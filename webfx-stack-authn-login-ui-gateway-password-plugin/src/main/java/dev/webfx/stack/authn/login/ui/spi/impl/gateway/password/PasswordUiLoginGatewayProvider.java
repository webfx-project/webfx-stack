package dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;

import dev.webfx.extras.util.scene.SceneUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayProviderBase;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.session.state.client.fx.FXUserId;
import dev.webfx.stack.ui.controls.MaterialFactoryMixin;
import dev.webfx.stack.ui.controls.button.ButtonFactory;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class PasswordUiLoginGatewayProvider extends UiLoginGatewayProviderBase implements MaterialFactoryMixin {

   private final static String GATEWAY_ID = "Password";

   private static Consumer<String> CREATE_ACCOUNT_EMAIL_CONSUMER;

   private UILoginView uiLoginView;

   private UiLoginPortalCallback uiLoginPortalcallback;

    // SignInMode = true => username/password, false => magic link
    private final Property<Boolean> signInModeProperty = new SimpleObjectProperty<>(true);

    public static void setCreateAccountEmailConsumer(Consumer<String> createAccountEmailConsumer) {
        CREATE_ACCOUNT_EMAIL_CONSUMER = createAccountEmailConsumer;
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
        uiLoginPortalcallback = callback;
        uiLoginView = new UILoginView(CREATE_ACCOUNT_EMAIL_CONSUMER);
        uiLoginView.initializeComponents();


        FXProperties.runNowAndOnPropertiesChange(() -> {
            boolean signIn = signInModeProperty.getValue();
            if (!signIn) {
                uiLoginView.transformPaneToForgetPasswordState(callback);
            }
            else {
                uiLoginView.transformPaneToInitialState(callback);
            }
        }, signInModeProperty);
        FXProperties.runNowAndOnPropertiesChange(this::prepareShowing, uiLoginView.getContainer().sceneProperty());
        return uiLoginView.getContainer();
    }

    /*private void initValidation() {
        validationSupport.addRequiredInput(usernameField, "Username is required");
        validationSupport.addRequiredInput(passwordField, "Password is required");
    }*/

    private void resetUXToLogin() {
        signInModeProperty.setValue(true);
        uiLoginView.transformPaneToInitialState(uiLoginPortalcallback);
    }

    public void prepareShowing() {
        I18nControls.bindI18nProperties(uiLoginView.getActionButton(), signInModeProperty.getValue() ? "Continue" : "SendLink>>");
        // Resetting the default button (required for JavaFX if displayed a second time)
        ButtonFactory.resetDefaultButton(uiLoginView.getActionButton());
        SceneUtil.autoFocusIfEnabled(uiLoginView.getEmailTextField());
        UiScheduler.scheduleDelay(500, () -> SceneUtil.autoFocusIfEnabled(uiLoginView.getEmailTextField()));
    }
}
