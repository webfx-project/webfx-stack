package dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;

import dev.webfx.extras.controlfactory.MaterialFactoryMixin;
import dev.webfx.extras.controlfactory.button.ButtonFactory;
import dev.webfx.extras.i18n.I18nKeys;
import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.util.scene.SceneUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginGatewayBase;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import dev.webfx.stack.session.state.client.fx.FXUserId;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class PasswordUiLoginGateway extends UiLoginGatewayBase implements MaterialFactoryMixin {

   private final static String GATEWAY_ID = "Password";

   private static Consumer<String> CREATE_ACCOUNT_EMAIL_CONSUMER;

   private UILoginView uiLoginView;
   private UiLoginPortalCallback uiLoginPortalcallback;
    // SignInMode = true => username/password, false => magic link
    private final BooleanProperty signInModeProperty = new SimpleBooleanProperty(true);

    public static void setCreateAccountEmailConsumer(Consumer<String> createAccountEmailConsumer) {
        CREATE_ACCOUNT_EMAIL_CONSUMER = createAccountEmailConsumer;
    }

    public PasswordUiLoginGateway() {
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

        FXProperties.runNowAndOnPropertyChange(signIn -> {
            if (signIn)
                uiLoginView.transformPaneToInitialState(callback);
            else
                uiLoginView.transformPaneToForgetPasswordState(callback);
        }, signInModeProperty);
        FXProperties.runNowAndOnPropertiesChange(this::prepareShowing, uiLoginView.getContainer().sceneProperty());
        return uiLoginView.getContainer();
    }

    private void resetUXToLogin() {
        signInModeProperty.setValue(true);
        // We wait for 1 s to reset the UXLogin, otherwise it changes too quickly, and we notice it on the UI if we go
        // from the password page to the home page (which take 1 s) => this change occurs between the two, which is
        // noticeable, and we don't want it.
        UiScheduler.scheduleDelay(1000, () -> uiLoginView.transformPaneToInitialState(uiLoginPortalcallback));
    }

    public void prepareShowing() {
        I18nControls.bindI18nProperties(uiLoginView.getActionButton(), signInModeProperty.getValue() ? PasswordI18nKeys.Continue : I18nKeys.appendArrows(PasswordI18nKeys.SendLink));
        // Resetting the default button (required for JavaFX if displayed a second time)
        ButtonFactory.resetDefaultButton(uiLoginView.getActionButton());
        SceneUtil.autoFocusIfEnabled(uiLoginView.getEmailTextField());
        UiScheduler.scheduleDelay(500, () -> SceneUtil.autoFocusIfEnabled(uiLoginView.getEmailTextField()));
    }
}
