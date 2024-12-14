package dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;


import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.windowlocation.WindowLocation;
import dev.webfx.stack.authn.AuthenticateWithUsernamePasswordCredentials;
import dev.webfx.stack.authn.AuthenticationRequest;
import dev.webfx.stack.authn.InitiateAccountCreationCredentials;
import dev.webfx.stack.authn.SendMagicLinkCredentials;
import dev.webfx.stack.authn.login.ui.FXLoginContext;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import dev.webfx.stack.i18n.I18n;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.ui.controls.MaterialFactoryMixin;
import dev.webfx.stack.ui.operation.OperationUtil;
import dev.webfx.stack.ui.validation.ValidationSupport;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;

import java.util.function.Consumer;

public class UILoginView implements MaterialFactoryMixin {

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
    private Consumer<String> createAccountEmailConsumer;


    private static final String CHECKMARK_PATH = "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z M14.7 8.39l-3.78 5-1.63-2.11a1 1 0 0 0-1.58 1.23l2.43 3.11a1 1 0 0 0 .79.38 1 1 0 0 0 .79-.39l4.57-6a1 1 0 1 0-1.6-1.22z";
    private boolean validationSupportInitialised;

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
        mainMessageLabel.setPadding(new Insets(40, 0, 0, 0));
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
        emailTextField.setMaxWidth(370);
        emailTextField.setMinWidth(370);


        passwordFieldAndMessageVbox = new VBox(10);
        passwordField = newMaterialPasswordField(PasswordI18nKeys.Password);
        passwordField.setMaxWidth(300);
        passwordField.setMaxWidth(370);
        VBox.setMargin(passwordField, new Insets(15, 0, 0, 0));
        passwordFieldAndMessageVbox.setMaxWidth(370);
        passwordFieldAndMessageVbox.setMinWidth(370);


        infoMessageForPasswordFieldLabel = Bootstrap.small(I18nControls.newLabel(PasswordI18nKeys.CaseSensitive));
        infoMessageForPasswordFieldLabel.setVisible(true);
        passwordFieldAndMessageVbox.getChildren().addAll(passwordField, infoMessageForPasswordFieldLabel);

        emailAndPasswordContainer.getChildren().setAll(emailTextField, passwordFieldAndMessageVbox);
        forgetRememberPasswordHyperlink = Bootstrap.textSecondary(I18nControls.newHyperlink(PasswordI18nKeys.GoToLogin));

        VBox.setMargin(forgetRememberPasswordHyperlink, new Insets(40, 0, 0, 0));

        createAccountHyperlink = new Hyperlink();
        //Here we display in a transition pane the content
        createAccountHyperlink.setOnAction(e -> {
        });
        I18nControls.bindI18nProperties(createAccountHyperlink, PasswordI18nKeys.CreateAccount);
        createAccountHyperlink.getStyleClass().setAll(Bootstrap.TEXT_SECONDARY);
        createAccountHyperlink.setVisible(true);
        Node createAccountContainer = new Region();
        if (createAccountEmailConsumer != null) {
            createAccountHyperlink.setVisible(true);
            createAccountHyperlink.setManaged(true);
        } else {
            createAccountHyperlink.setVisible(false);
            createAccountHyperlink.setManaged(false);
        }
        createAccountHyperlink.setOnAction(null);
        VBox.setMargin(createAccountHyperlink, new Insets(20, 0, 0, 0));

        Label createAccountLabel = I18nControls.bindI18nProperties(new Label(), PasswordI18nKeys.CreateAccount);

        actionButton = Bootstrap.largePrimaryButton(I18nControls.newButton(PasswordI18nKeys.Continue));
        VBox.setMargin(actionButton, new Insets(30, 0, 0, 0));
        mainVBox.getChildren().addAll(loginTitleLabel, mainMessageLabel, emailAndPasswordContainer, forgetRememberPasswordHyperlink, createAccountHyperlink, actionButton);
    }

    private void initFormValidation() {
        if (!validationSupportInitialised) {
            FXProperties.runNowAndOnPropertyChange(dictionary -> {
                if (dictionary != null) {
                    validationSupport.reset();
                    validationSupport.addEmailValidation(emailTextField, emailTextField, I18n.getI18nText(PasswordI18nKeys.InvalidEmail));
                    validationSupport.addRequiredInput(emailTextField);
                }
            }, I18n.dictionaryProperty());
            validationSupportInitialised = true;
        }
    }

    public boolean validateForm() {
        if (!validationSupportInitialised) {
            initFormValidation();
            validationSupportInitialised = true;
        }
        return validationSupport.isValid();
    }

    private void transformPaneToCreateAccount(UiLoginPortalCallback callback) {
        hidePasswordField();
        hideMessageForPasswordField();
        I18nControls.bindI18nProperties(forgetRememberPasswordHyperlink, PasswordI18nKeys.Back);
        showForgetPasswordHyperlink();
        forgetRememberPasswordHyperlink.setOnAction(e->{transformPaneToInitialState(callback);});
        loginTitleLabel.setWrapText(true);
        I18nControls.bindI18nProperties(mainMessageLabel, PasswordI18nKeys.CreateAccountInfoMessage);
        showMainMessage();
        I18nControls.bindI18nProperties(loginTitleLabel, PasswordI18nKeys.CreateAccountTitle);
        hideCreateAccountHyperlink();
        I18nControls.bindI18nProperties(actionButton, PasswordI18nKeys.SendEmailToValidate);
        actionButton.setOnAction(event -> {
            if (validateForm()) {
                Object credentials = new InitiateAccountCreationCredentials(emailTextField.getText(), WindowLocation.getOrigin(), WindowLocation.getPath(), I18n.getLanguage(), FXLoginContext.getLoginContext());
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
        mainMessageLabel.setManaged(false);
        hideMessageForPasswordField();
        hideForgetPasswordHyperlink();
        I18nControls.bindI18nProperties(actionButton, PasswordI18nKeys.Continue);
        showCreateAccountHyperlink();
        createAccountHyperlink.setOnAction(e->{transformPaneToCreateAccount(callback);});
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
        mainMessageLabel.setManaged(true);
        showForgetPasswordHyperlink();
        I18nControls.bindI18nProperties(loginTitleLabel, PasswordI18nKeys.Login);
        hideCreateAccountHyperlink();
        I18nControls.bindI18nProperties(actionButton, PasswordI18nKeys.Login);
        passwordField.requestFocus();

        I18nControls.bindI18nProperties(forgetRememberPasswordHyperlink, PasswordI18nKeys.ForgotPassword);
        forgetRememberPasswordHyperlink.setOnAction(e -> transformPaneToForgetPasswordState(callback));

        actionButton.setOnAction(event -> {
            if (validateForm()) {
                Object credentials = new AuthenticateWithUsernamePasswordCredentials(emailTextField.getText(), passwordField.getText());
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
                        emailTextField.clear();
                        passwordField.clear();
                        hideGraphicFromActionButton();
                    }));
            }
        });
        moveActionButtonAtTheBottom();
    }




    public void transformPaneToForgetPasswordState(UiLoginPortalCallback callback) {
        hidePasswordField();
        hideMessageForPasswordField();
        mainMessageLabel.setManaged(true);
        I18nControls.bindI18nProperties(loginTitleLabel, PasswordI18nKeys.Recovery);
        hideCreateAccountHyperlink();
        I18nControls.bindI18nProperties(forgetRememberPasswordHyperlink, PasswordI18nKeys.RememberPassword);
        forgetRememberPasswordHyperlink.setOnAction(e -> transformPaneToLoginAndPasswordState(callback));
        I18nControls.bindI18nProperties(actionButton, PasswordI18nKeys.SendLink+">>");

        actionButton.setOnAction(event -> {
            if(validateForm()) {
                Object credentials = new SendMagicLinkCredentials(emailTextField.getText(), WindowLocation.getOrigin(), WindowLocation.getPath(), I18n.getLanguage(), FXLoginContext.getLoginContext());
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

    public void setInfoMessageForPasswordFieldLabel(String I18nKey, String bootStrapStyle) {
        I18nControls.bindI18nProperties(infoMessageForPasswordFieldLabel, I18nKey);
        infoMessageForPasswordFieldLabel.getStyleClass().setAll(bootStrapStyle);
    }

    public void setForgetRememberPasswordHyperlink(String I18nKey) {
        I18nControls.bindI18nProperties(forgetRememberPasswordHyperlink, I18nKey);
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

    public void setMainMessage(String I18nKey, String bootStrapStyle) {
        I18nControls.bindI18nProperties(mainMessageLabel, I18nKey);
        mainMessageLabel.getStyleClass().setAll(bootStrapStyle);
    }

    public void setLabelOnActionButton(String I18nKey) {
        I18nControls.bindI18nProperties(actionButton, I18nKey);
    }

    public void setTitle(String I18nKey) {
        I18nControls.bindI18nProperties(loginTitleLabel, I18nKey);
    }

    public void showMainMessage() {
        mainMessageLabel.setVisible(true);
        mainMessageLabel.setManaged(true);
    }

    public void hideMainMessage() {
        mainMessageLabel.setVisible(false);
        // mainMessageLabel.setManaged(false);
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

    public Hyperlink getForgetRememberPasswordHyperlink() {
        return forgetRememberPasswordHyperlink;
    }

    public Label getInfoMessageForPasswordFieldLabel() {
        return infoMessageForPasswordFieldLabel;
    }

    public Label getLoginTitleLabel() {
        return loginTitleLabel;
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
