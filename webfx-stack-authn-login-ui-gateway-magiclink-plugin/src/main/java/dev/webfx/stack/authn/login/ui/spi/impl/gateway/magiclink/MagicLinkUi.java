package dev.webfx.stack.authn.login.ui.spi.impl.gateway.magiclink;

import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.*;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.PasswordI18nKeys;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.password.UILoginView;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.ui.controls.MaterialFactoryMixin;
import dev.webfx.stack.ui.operation.OperationUtil;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public class MagicLinkUi implements MaterialFactoryMixin {

    private final UILoginView uiLoginView;
    private String pathToBeRedirected;
    private final StringProperty tokenProperty;
    private final Consumer<String> pathConsumer;

    public MagicLinkUi(StringProperty tokenProperty, Consumer<String> requestedPathConsumer) {
        this.tokenProperty = tokenProperty;
        pathConsumer = requestedPathConsumer;
        uiLoginView = new UILoginView();
        uiLoginView.initializeComponents();
        uiLoginView.setTitle(MagicLinkI18nKeys.Recovery);
        uiLoginView.setMainMessage(MagicLinkI18nKeys.ChangeYourPassword, Bootstrap.STRONG);
        uiLoginView.setLabelOnActionButton(MagicLinkI18nKeys.ConfirmChange);
        uiLoginView.showMainMessage();
        uiLoginView.hideEmailField();
        uiLoginView.hideHyperlink();
        uiLoginView.showMessageForPasswordField();
        uiLoginView.hideGraphicFromActionButton();
        Button actionButton = uiLoginView.getActionButton();
        actionButton.setDisable(false);

        FXProperties.runNowAndOnPropertyChange(token -> {
            if (token == null) {
                I18n.bindI18nProperties(new Text(), MagicLinkI18nKeys.MagicLinkUnrecognisedError);
            } else {
                AuthenticationService.authenticate(new AuthenticateWithMagicLinkCredentials(token))
                    .onFailure(e -> UiScheduler.runInUiThread(() -> onFailure(e)))
                    .onSuccess(requestedPath -> {
                        UiScheduler.runInUiThread(this::onSuccess);
                        pathToBeRedirected = requestedPath.toString();
                    });
            }
        }, tokenProperty);
    }

    public Node getUi() {
        return uiLoginView.getContainer();
    }

    private void onSuccess() {
        uiLoginView.setTitle(MagicLinkI18nKeys.Recovery);
        uiLoginView.setMainMessage(MagicLinkI18nKeys.ChangeYourPassword,Bootstrap.STRONG);
        uiLoginView.setLabelOnActionButton(MagicLinkI18nKeys.ConfirmChange);
        uiLoginView.showMainMessage();
        uiLoginView.hideEmailField();
        uiLoginView.hideHyperlink();
        uiLoginView.showMessageForPasswordField();
        uiLoginView.showPasswordField();
        uiLoginView.showMessageForPasswordField();
        uiLoginView.hideGraphicFromActionButton();
        uiLoginView.getActionButton().setDisable(false);
        uiLoginView.getActionButton().setOnAction(l -> AuthenticationService.updateCredentials(new UpdatePasswordFromMagicLinkCredentials(uiLoginView.getPasswordField().getText()))
            .onFailure(e -> {
                Console.log("Error Updating password: " + e);
                Platform.runLater(()->onFailure(e));
            })
            .onSuccess(ignored -> {
                Console.log("Password Updated");
                Platform.runLater(()->{
                    uiLoginView.setMainMessage(PasswordI18nKeys.PasswordUpdated,Bootstrap.TEXT_SUCCESS);
                    uiLoginView.showMainMessage();
                    uiLoginView.getActionButton().setDisable(true);
                    uiLoginView.getPasswordField().setDisable(true);
                    uiLoginView.setHyperlink(MagicLinkI18nKeys.BackToNavigation);
                    uiLoginView.showHyperlink();
                    uiLoginView.getHyperlink().setOnAction(e2-> pathConsumer.accept(pathToBeRedirected));
                    uiLoginView.showGraphicFromActionButton();
                });
            }));
    }

    private void onFailure(Throwable e) {
        String technicalMessage = e.getMessage();
        Console.log("Technical error: " + technicalMessage);

        if (technicalMessage != null) {
            //The error Message are defined in ModalityMagicLinkAuthenticationGatewayProvider
            if (technicalMessage.contains("not found")) {
                uiLoginView.getInfoMessageForPasswordFieldLabel().setVisible(false);
                uiLoginView.setTitle(MagicLinkI18nKeys.MagicLinkUnrecognisedErrorTitle);
                uiLoginView.setMainMessage(MagicLinkI18nKeys.MagicLinkUnexpectedError, Bootstrap.STRONG);
                uiLoginView.displayOnlyTitleAndMainMessage();
            }
            else if (technicalMessage.contains("used") || technicalMessage.contains("expired")) {
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
                uiLoginView.hideHyperlink();
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
                        .onFailure(failure->Console.log("Fail to renew Magik Link:" + failure.getMessage() ))
                        .onSuccess(ignored -> UiScheduler.runInUiThread(() -> {
                            uiLoginView.setMainMessage(PasswordI18nKeys.LinkSent,Bootstrap.TEXT_SUCCESS);
                            uiLoginView.showMainMessage();
                            uiLoginView.getActionButton().setDisable(true);
                            uiLoginView.getEmailTextField().setDisable(true);
                            uiLoginView.hideHyperlink();
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

}
