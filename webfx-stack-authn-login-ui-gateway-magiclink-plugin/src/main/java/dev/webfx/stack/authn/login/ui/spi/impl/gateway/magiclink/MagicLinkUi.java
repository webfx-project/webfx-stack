package dev.webfx.stack.authn.login.ui.spi.impl.gateway.magiclink;

import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.*;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.PasswordI18nKeys;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.UILoginView;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.ui.controls.MaterialFactoryMixin;
import dev.webfx.stack.ui.operation.OperationUtil;
import dev.webfx.stack.ui.validation.ValidationSupport;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;

import java.util.function.Consumer;

/**
 * @author David Hello
 */
public class MagicLinkUi implements MaterialFactoryMixin {

    private final UILoginView uiLoginView;
    private final StringProperty tokenProperty;
    private final Consumer<String> requestedPathConsumer;
    private final ValidationSupport validationSupport = new ValidationSupport();

    public MagicLinkUi(StringProperty tokenProperty, Consumer<String> requestedPathConsumer) {
        this.tokenProperty = tokenProperty;
        this.requestedPathConsumer = requestedPathConsumer;
        uiLoginView = new UILoginView(null);
        uiLoginView.initializeComponents();
        uiLoginView.setTitle(MagicLinkI18nKeys.Recovery);
        uiLoginView.setMainMessage(MagicLinkI18nKeys.ChangeYourPassword, Bootstrap.STRONG);
        uiLoginView.setLabelOnActionButton(MagicLinkI18nKeys.ConfirmChange);
        uiLoginView.showMainMessage();
        uiLoginView.hideEmailField();
        uiLoginView.hideForgetPasswordHyperlink();
        uiLoginView.showMessageForPasswordField();
        uiLoginView.hideGraphicFromActionButton();
        Button actionButton = uiLoginView.getActionButton();
        actionButton.setDisable(false);
        actionButton.setDefaultButton(true);

        FXProperties.runNowAndOnPropertyChange(token -> {
            if (token == null) {
                uiLoginView.setMainMessage(MagicLinkI18nKeys.MagicLinkUnrecognisedError, Bootstrap.TEXT_DANGER);
            } else {
                AuthenticationService.authenticate(new AuthenticateWithMagicLinkCredentials(token))
                    .onFailure(e -> UiScheduler.runInUiThread(() -> onFailure(e)))
                    .onSuccess(requestedPath -> UiScheduler.runInUiThread(() -> onSuccess((String) requestedPath)));
            }
        }, tokenProperty);
    }

    public Node getUi() {
        return uiLoginView.getContainer();
    }

    private void onSuccess(String requestedPath) {
        uiLoginView.setTitle(MagicLinkI18nKeys.Recovery);
        uiLoginView.setMainMessage(MagicLinkI18nKeys.ChangeYourPassword, Bootstrap.STRONG);
        uiLoginView.setLabelOnActionButton(MagicLinkI18nKeys.ConfirmChange);
        uiLoginView.showMainMessage();
        uiLoginView.hideEmailField();
        uiLoginView.hideForgetPasswordHyperlink();
        uiLoginView.showMessageForPasswordField();
        uiLoginView.showPasswordField();
        uiLoginView.showMessageForPasswordField();
        uiLoginView.hideGraphicFromActionButton();
        uiLoginView.getActionButton().setDisable(false);
        uiLoginView.getActionButton().setOnAction(l -> {
            if (validateForm()) {
                AuthenticationService.updateCredentials(new UpdatePasswordFromMagicLinkCredentials(uiLoginView.getPasswordField().getText()))
                    .onFailure(e -> {
                        Console.log("Error Updating password: " + e);
                        Platform.runLater(() -> onFailure(e));
                    })
                    .onSuccess(ignored -> {
                        Console.log("Password Updated");
                        Platform.runLater(() -> {
                            uiLoginView.setMainMessage(PasswordI18nKeys.PasswordUpdated, Bootstrap.TEXT_SUCCESS);
                            uiLoginView.showMainMessage();
                            I18nControls.bindI18nProperties(uiLoginView.getActionButton(), PasswordI18nKeys.GoToLogin);
                            uiLoginView.getActionButton().setOnAction(e2 -> requestedPathConsumer.accept(requestedPath));
                            uiLoginView.getPasswordField().setDisable(true);
                            uiLoginView.setForgetRememberPasswordHyperlink(MagicLinkI18nKeys.BackToNavigation);
                            uiLoginView.hideForgetPasswordHyperlink();
                            //uiLoginView.getForgetRememberPasswordHyperlink().setOnAction(e2-> pathConsumer.accept(pathToBeRedirected));
                            uiLoginView.hideGraphicFromActionButton();
                        });
                    });
            }
        });
    }


    private void onFailure(Throwable e) {
        String technicalMessage = e.getMessage();
        Console.log("Technical error: " + technicalMessage);

        if (technicalMessage != null) {
            //The technical error messages are defined in ModalityMagicLinkAuthenticationGatewayProvider
            if (technicalMessage.contains("not found")) {
                uiLoginView.getInfoMessageForPasswordFieldLabel().setVisible(false);
                uiLoginView.setTitle(MagicLinkI18nKeys.MagicLinkUnrecognisedErrorTitle);
                uiLoginView.setMainMessage(MagicLinkI18nKeys.MagicLinkUnexpectedError, Bootstrap.STRONG);
                uiLoginView.displayOnlyTitleAndMainMessage();
            } else if (technicalMessage.contains("used") || technicalMessage.contains("expired")) {
                if (technicalMessage.contains("used")) {
                    uiLoginView.setTitle(MagicLinkI18nKeys.MagicLinkAlreadyUsedErrorTitle);
                    uiLoginView.setMainMessage(MagicLinkI18nKeys.MagicLinkAlreadyUsedError, Bootstrap.STRONG);
                }
                if (technicalMessage.contains("expired")) {
                    uiLoginView.setTitle(MagicLinkI18nKeys.MagicLinkExpiredErrorTitle);
                    uiLoginView.setMainMessage(MagicLinkI18nKeys.MagicLinkExpiredError, Bootstrap.STRONG);
                }
                uiLoginView.setLabelOnActionButton(PasswordI18nKeys.SendLink);
                uiLoginView.showMainMessage();
                uiLoginView.hideForgetPasswordHyperlink();
                uiLoginView.hideMessageForPasswordField();
                uiLoginView.hidePasswordField();
                uiLoginView.showEmailField();
                uiLoginView.hideGraphicFromActionButton();
                uiLoginView.getActionButton().setDisable(false);
                uiLoginView.getActionButton().setOnAction(event -> {
                    Object credentials = new RenewMagicLinkCredentials(tokenProperty.get());
                    OperationUtil.turnOnButtonsWaitMode(uiLoginView.getActionButton());
                    new AuthenticationRequest()
                        .setUserCredentials(credentials)
                        .executeAsync()
                        .onComplete(ar -> UiScheduler.runInUiThread(() -> OperationUtil.turnOffButtonsWaitMode(uiLoginView.getActionButton())))
                        .onFailure(failure -> Console.log("Fail to renew Magik Link:" + failure.getMessage()))
                        .onSuccess(ignored -> UiScheduler.runInUiThread(() -> {
                            uiLoginView.setMainMessage(PasswordI18nKeys.LinkSent, Bootstrap.TEXT_SUCCESS);
                            uiLoginView.showMainMessage();
                            uiLoginView.getActionButton().setDisable(true);
                            uiLoginView.getEmailTextField().setDisable(true);
                            uiLoginView.hideForgetPasswordHyperlink();
                            uiLoginView.showGraphicFromActionButton();
                        }));
                });
            }
            if (technicalMessage.contains("address")) {
                uiLoginView.getInfoMessageForPasswordFieldLabel().setVisible(false);
                uiLoginView.setTitle(MagicLinkI18nKeys.MagicLinkPushErrorTitle);
                uiLoginView.setMainMessage(MagicLinkI18nKeys.MagicLinkPushError, Bootstrap.STRONG);
                uiLoginView.displayOnlyTitleAndMainMessage();
            }
            if (technicalMessage.contains("closed")) {
                uiLoginView.getInfoMessageForPasswordFieldLabel().setVisible(false);
                uiLoginView.setTitle(MagicLinkI18nKeys.MagicLinkBusClosedErrorTitle);
                uiLoginView.setMainMessage(MagicLinkI18nKeys.MagicLinkBusClosedError, Bootstrap.STRONG);
                uiLoginView.displayOnlyTitleAndMainMessage();
            }
        }
    }

    /**
     * This method is used to initialise the parameters for the form validation
     */
    private void initFormValidation() {
        if (validationSupport.isEmpty()) {
            validationSupport.addPasswordStrengthValidation(uiLoginView.getPasswordField(), I18n.i18nTextProperty(MagicLinkI18nKeys.PasswordStrength));
        }
    }

    /**
     * We validate the form
     *
     * @return true if all the validation is success, false otherwise
     */
    public boolean validateForm() {
        initFormValidation(); // does nothing if already initialised
        return validationSupport.isValid();
    }

}
