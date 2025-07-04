package dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;


import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.util.control.Controls;
import dev.webfx.extras.util.control.HtmlInputAutocomplete;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.windowlocation.WindowLocation;
import dev.webfx.stack.authn.AuthenticateWithUsernamePasswordCredentials;
import dev.webfx.stack.authn.AuthenticationRequest;
import dev.webfx.stack.authn.InitiateAccountCreationCredentials;
import dev.webfx.stack.authn.SendMagicLinkCredentials;
import dev.webfx.stack.authn.login.ui.FXLoginContext;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import dev.webfx.extras.i18n.I18n;
import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.controlfactory.MaterialFactoryMixin;
import dev.webfx.extras.operation.OperationUtil;
import dev.webfx.extras.validation.ValidationSupport;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;

import java.util.function.Consumer;

/**
 * @author David Hello
 */
public class UILoginView implements MaterialFactoryMixin {

    private static final String CHECKMARK_PATH = "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z M14.7 8.39l-3.78 5-1.63-2.11a1 1 0 0 0-1.58 1.23l2.43 3.11a1 1 0 0 0 .79.38 1 1 0 0 0 .79-.39l4.57-6a1 1 0 1 0-1.6-1.22z";

    private Label loginTitleLabel;
    private Label mainMessageLabel;
    private TextField emailTextField;
    private PasswordField passwordField;
    private Label infoMessageForPasswordFieldLabel;
    private Hyperlink forgetRememberPasswordHyperlink;
    private Hyperlink createAccountHyperlink;
    private Button actionButton;
    private VBox mainVBox;
    private VBox passwordFieldAndMessageVbox;
    private VBox emailAndPasswordContainer;
    private final ValidationSupport validationSupport = new ValidationSupport();
    private BorderPane container;
    private final Consumer<String> createAccountEmailConsumer;

    public UILoginView(Consumer<String> emailConsumer) {
        createAccountEmailConsumer = emailConsumer;
    }

    public void initializeComponents() {
        container = new BorderPane();
        container.setMaxWidth(Double.MAX_VALUE); // so it fills the whole width of the main frame VBox (with text centered)
        VBox loginVBox = new VBox();
        loginVBox.setAlignment(Pos.CENTER);
        initialiseMainVBox(loginVBox);
        loginVBox.getChildren().addAll(mainVBox);
        loginVBox.setSpacing(60);
        container.setCenter(loginVBox);
    }

    private void initialiseMainVBox(VBox container) {
        mainVBox = new VBox();
        mainVBox.setMinWidth(container.getMinWidth());
        //mainVBox.getStyleClass().add("login");
        mainVBox.setAlignment(Pos.TOP_CENTER);

        loginTitleLabel = Bootstrap.h2Primary(I18nControls.newLabel(PasswordI18nKeys.Recovery));
        int vSpacing = 10;
        loginTitleLabel.setPadding(new Insets(vSpacing, 0, 0, 0));

        mainMessageLabel = Bootstrap.textSuccess(new Label("Success Message"));
        mainMessageLabel.setPadding(new Insets(60, 0, 0, 0));
        mainMessageLabel.setTextAlignment(TextAlignment.CENTER);
        mainMessageLabel.setWrapText(true);
        mainMessageLabel.setGraphicTextGap(15);
        hideMainMessage();

        emailAndPasswordContainer = new VBox();
        emailAndPasswordContainer.setAlignment(Pos.CENTER);
        int vBoxHeight = 150;
        emailAndPasswordContainer.setMinHeight(vBoxHeight);
        emailAndPasswordContainer.setMaxHeight(vBoxHeight);
        emailTextField = newMaterialTextField(PasswordI18nKeys.Email);
        VBox.setMargin(emailTextField, new Insets(40, 0, 0, 0));
        emailTextField.setPrefWidth(370);
        Controls.setHtmlInputTypeAndAutocompleteToEmail(emailTextField);

        passwordFieldAndMessageVbox = new VBox(10);
        passwordField = newMaterialPasswordField(PasswordI18nKeys.Password);
        Controls.setHtmlInputAutocomplete(passwordField, HtmlInputAutocomplete.CURRENT_PASSWORD);
        passwordField.setPrefWidth(370);
        VBox.setMargin(passwordField, new Insets(15, 0, 0, 0));

        infoMessageForPasswordFieldLabel = Bootstrap.small(I18nControls.newLabel(PasswordI18nKeys.CaseSensitive));
        infoMessageForPasswordFieldLabel.setVisible(true);
        passwordFieldAndMessageVbox.getChildren().addAll(passwordField, infoMessageForPasswordFieldLabel);

        emailAndPasswordContainer.getChildren().setAll(emailTextField, passwordFieldAndMessageVbox);
        forgetRememberPasswordHyperlink = Bootstrap.textSecondary(I18nControls.newHyperlink(PasswordI18nKeys.GoToLogin));

        VBox.setMargin(forgetRememberPasswordHyperlink, new Insets(40, 0, 0, 0));

        createAccountHyperlink = I18nControls.newHyperlink(PasswordI18nKeys.CreateAccount);
        if (createAccountEmailConsumer == null) {
            hideCreateAccountHyperlink();
        }
        VBox.setMargin(createAccountHyperlink, new Insets(20, 0, 0, 0));

        actionButton = Bootstrap.largePrimaryButton(I18nControls.newButton(PasswordI18nKeys.Continue));
        VBox.setMargin(actionButton, new Insets(30, 0, 0, 0));
        mainVBox.getChildren().addAll(loginTitleLabel, mainMessageLabel, emailAndPasswordContainer, forgetRememberPasswordHyperlink, createAccountHyperlink, actionButton);
    }

    private void initFormValidation() {
        if (validationSupport.isEmpty()) {
            validationSupport.addEmailValidation(emailTextField, emailTextField, I18n.i18nTextProperty(PasswordI18nKeys.InvalidEmail));
            validationSupport.addRequiredInput(emailTextField);
        }
    }

    public boolean validateForm() {
        initFormValidation();
        return validationSupport.isValid();
    }

    private void transformPaneToCreateAccount(UiLoginPortalCallback callback) {
        hidePasswordField();
        hideMessageForPasswordField();
        I18nControls.bindI18nProperties(forgetRememberPasswordHyperlink, PasswordI18nKeys.Back);
        showForgetPasswordHyperlink();
        forgetRememberPasswordHyperlink.setOnAction(e-> transformPaneToInitialState(callback));
        loginTitleLabel.setWrapText(true);
        I18nControls.bindI18nProperties(mainMessageLabel, PasswordI18nKeys.CreateAccountInfoMessage);
        showMainMessage();
        I18nControls.bindI18nProperties(loginTitleLabel, PasswordI18nKeys.CreateAccountTitle);
        hideCreateAccountHyperlink();
        I18nControls.bindI18nProperties(actionButton, PasswordI18nKeys.SendEmailToValidate);
        actionButton.setOnAction(event -> {
            if (validateForm()) {
                Object credentials = new InitiateAccountCreationCredentials(emailTextField.getText().trim().toLowerCase(), WindowLocation.getOrigin(), WindowLocation.getPath(), I18n.getLanguage(), FXLoginContext.getLoginContext());
                OperationUtil.turnOnButtonsWaitMode(actionButton);
                new AuthenticationRequest()
                    .setUserCredentials(credentials)
                    .executeAsync()
                    .onComplete(ar -> UiScheduler.runInUiThread(() -> OperationUtil.turnOffButtonsWaitMode(actionButton)))
                    .onFailure(failure -> {
                        callback.notifyUserLoginFailed(failure);
                        Platform.runLater(() -> {
                            setInfoMessageForPasswordFieldLabel(PasswordI18nKeys.ErrorOccurred, Bootstrap.TEXT_DANGER);
                            showMessageForPasswordField();
                        });
                    })
                    .onSuccess(ignored -> UiScheduler.runInUiThread(() -> {
                        I18nControls.bindI18nProperties(mainMessageLabel, PasswordI18nKeys.AccountCreationLinkSent);
                        mainMessageLabel.setPadding(new Insets(100,15,0,15));
                        showMainMessage();
                        Label closeWindowLabel = Bootstrap.textSecondary(I18nControls.newLabel(PasswordI18nKeys.YouMayCloseThisWindow));
                        closeWindowLabel.setPadding(new Insets(100,15,0,15));
                        closeWindowLabel.setWrapText(true);
                        int indexToInsert = mainVBox.getChildren().indexOf(mainMessageLabel) + 1;
                        mainVBox.getChildren().add(indexToInsert, closeWindowLabel);
                        hideActionButton();
                        actionButton.setManaged(false);
                        hideEmailField();
                        emailTextField.setManaged(false);
                        hidePasswordField();
                        passwordField.setManaged(false);
                        hideForgetPasswordHyperlink();
                        forgetRememberPasswordHyperlink.setManaged(false);
                    }));
            }
        });
    }

    public void transformPaneToInitialState(UiLoginPortalCallback callback) {
        I18nControls.bindI18nProperties(loginTitleLabel, PasswordI18nKeys.Login);
        hidePasswordField();
        hideMainMessage();
        mainMessageLabel.setManaged(false);
        hideMessageForPasswordField();
        hideForgetPasswordHyperlink();
        I18nControls.bindI18nProperties(actionButton, PasswordI18nKeys.Continue);
        showCreateAccountHyperlink();
        createAccountHyperlink.setOnAction(e-> transformPaneToCreateAccount(callback));
        actionButton.setOnAction(e->{
            if (validateForm()) {
                transformPaneToLoginAndPasswordState(callback);
            }
        });
        moveActionButtonUnderEmail();
    }

    private void moveActionButtonUnderEmail() {
        mainVBox.getChildren().setAll(loginTitleLabel, mainMessageLabel, emailAndPasswordContainer, actionButton, forgetRememberPasswordHyperlink, createAccountHyperlink);
    }

    private void moveActionButtonAtTheBottom() {
        mainVBox.getChildren().setAll(loginTitleLabel, mainMessageLabel, emailAndPasswordContainer, forgetRememberPasswordHyperlink, createAccountHyperlink, actionButton);
    }

    public void transformPaneToLoginAndPasswordState(UiLoginPortalCallback callback) {
        showPasswordField();
        showMessageForPasswordField();
        showForgetPasswordHyperlink();
        I18nControls.bindI18nProperties(loginTitleLabel, PasswordI18nKeys.Login);
        hideCreateAccountHyperlink();
        I18nControls.bindI18nProperties(actionButton, PasswordI18nKeys.Login);
        passwordField.requestFocus();

        I18nControls.bindI18nProperties(forgetRememberPasswordHyperlink, PasswordI18nKeys.ForgotPassword);
        forgetRememberPasswordHyperlink.setOnAction(e -> transformPaneToForgetPasswordState(callback));

        actionButton.setOnAction(event -> {
            if (validateForm()) {
                Object credentials = new AuthenticateWithUsernamePasswordCredentials(emailTextField.getText().trim().toLowerCase(), passwordField.getText().trim());
                OperationUtil.turnOnButtonsWaitMode(actionButton);
                new AuthenticationRequest()
                    .setUserCredentials(credentials)
                    .executeAsync()
                    .onComplete(ar -> UiScheduler.runInUiThread(() -> OperationUtil.turnOffButtonsWaitMode(actionButton)))
                    .onFailure(failure -> {
                        callback.notifyUserLoginFailed(failure);
                        Platform.runLater(() -> {
                            setInfoMessageForPasswordFieldLabel(PasswordI18nKeys.IncorrectLoginOrPassword, Bootstrap.TEXT_DANGER);
                            showMessageForPasswordField();
                        });
                    })
                    .onSuccess(ignored -> UiScheduler.scheduleDelay(1000,() -> passwordField.setText("")));
            }
        });
        moveActionButtonAtTheBottom();
    }




    public void transformPaneToForgetPasswordState(UiLoginPortalCallback callback) {
        hidePasswordField();
        hideMessageForPasswordField();
        I18nControls.bindI18nProperties(loginTitleLabel, PasswordI18nKeys.Recovery);
        hideCreateAccountHyperlink();
        I18nControls.bindI18nProperties(forgetRememberPasswordHyperlink, PasswordI18nKeys.RememberPassword);
        forgetRememberPasswordHyperlink.setOnAction(e -> transformPaneToLoginAndPasswordState(callback));
        I18nControls.bindI18nProperties(actionButton, PasswordI18nKeys.SendLink+">>");

        actionButton.setOnAction(event -> {
            if(validateForm()) {
                Object credentials = new SendMagicLinkCredentials(emailTextField.getText().trim().toLowerCase(), WindowLocation.getOrigin(), WindowLocation.getPath(), I18n.getLanguage(), FXLoginContext.getLoginContext());
                OperationUtil.turnOnButtonsWaitMode(actionButton);
                new AuthenticationRequest()
                    .setUserCredentials(credentials)
                    .executeAsync()
                    .onComplete(ar -> UiScheduler.runInUiThread(() -> OperationUtil.turnOffButtonsWaitMode(actionButton)))
                    .onFailure(failure -> {
                        callback.notifyUserLoginFailed(failure);
                        Platform.runLater(() -> {
                            setInfoMessageForPasswordFieldLabel(PasswordI18nKeys.IncorrectLoginOrPassword, Bootstrap.TEXT_DANGER);
                            showMessageForPasswordField();
                        });
                    })
                    .onSuccess(ignored -> UiScheduler.runInUiThread(() -> {
                        setTitle(PasswordI18nKeys.Recovery);
                        setMainMessage(PasswordI18nKeys.LinkSent, Bootstrap.TEXT_SUCCESS);
                        showMainMessage();
                        actionButton.setDisable(true);
                        emailTextField.setDisable(true);
                        hidePasswordField();
                        hideForgetPasswordHyperlink();
                        showGraphicFromActionButton();
                    }));
            }
        });
    }


    public void hideCreateAccountHyperlink() {
        createAccountHyperlink.setVisible(false);
        createAccountHyperlink.setManaged(false);
    }


     public void showCreateAccountHyperlink() {
         createAccountHyperlink.setVisible(true);
         createAccountHyperlink.setManaged(true);
    }


    public void hideForgetPasswordHyperlink() {
        forgetRememberPasswordHyperlink.setVisible(false);
    }

    public void showForgetPasswordHyperlink() {
        forgetRememberPasswordHyperlink.setVisible(true);
    }

    public void setInfoMessageForPasswordFieldLabel(Object i18nKey, String bootStrapStyle) {
        I18nControls.bindI18nProperties(infoMessageForPasswordFieldLabel, i18nKey);
        infoMessageForPasswordFieldLabel.getStyleClass().setAll(bootStrapStyle);
    }

    public void setForgetRememberPasswordHyperlink(Object i18nKey) {
        I18nControls.bindI18nProperties(forgetRememberPasswordHyperlink, i18nKey);
    }

    public void hideMessageForPasswordField() {
        infoMessageForPasswordFieldLabel.setVisible(false);
    }

    public void showMessageForPasswordField() {
        infoMessageForPasswordFieldLabel.setVisible(true);
    }

    public void hideEmailField() {
        emailTextField.setVisible(false);
        emailTextField.setManaged(false);
    }

    public void showEmailField() {
        emailTextField.setVisible(true);
        emailTextField.setManaged(true);
    }

    public void setMainMessage(Object i18nKey, String bootStrapStyle) {
        I18nControls.bindI18nProperties(mainMessageLabel, i18nKey);
        mainMessageLabel.getStyleClass().setAll(bootStrapStyle);
    }

    public void setLabelOnActionButton(Object i18nKey) {
        I18nControls.bindI18nProperties(actionButton, i18nKey);
    }

    public void setTitle(Object i18nKey) {
        I18nControls.bindI18nProperties(loginTitleLabel, i18nKey);
    }

    public void showMainMessage() {
        mainMessageLabel.setVisible(true);
        mainMessageLabel.setManaged(true);
    }

    public void hideMainMessage() {
        mainMessageLabel.setVisible(false);
        mainMessageLabel.setManaged(false);
    }

    public void showPasswordField() {
        getPasswordFieldAndMessageVbox().setVisible(true);
        getPasswordFieldAndMessageVbox().setManaged(true);
    }

    public void hidePasswordField() {
        getPasswordFieldAndMessageVbox().setVisible(false);
        getPasswordFieldAndMessageVbox().setManaged(false);
    }

    public void hideGraphicFromActionButton() {
        actionButton.setGraphic(null);
    }

    public void showGraphicFromActionButton() {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(CHECKMARK_PATH);
        actionButton.setGraphic(new ScalePane(svgPath));
    }

    public void hideActionButton() {
        actionButton.setVisible(false);
    }

    public Button getActionButton() {
        return actionButton;
    }

    public void displayOnlyTitleAndMainMessage() {
        showMainMessage();
        hideEmailField();
        hidePasswordField();
        hideMessageForPasswordField();
        hideForgetPasswordHyperlink();
        hideActionButton();
    }


    public Label getInfoMessageForPasswordFieldLabel() {
        return infoMessageForPasswordFieldLabel;
    }


    public BorderPane getContainer() {
        return container;
    }

    public TextField getEmailTextField() {
        return emailTextField;
    }

    public VBox getPasswordFieldAndMessageVbox() {
        return passwordFieldAndMessageVbox;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

}
