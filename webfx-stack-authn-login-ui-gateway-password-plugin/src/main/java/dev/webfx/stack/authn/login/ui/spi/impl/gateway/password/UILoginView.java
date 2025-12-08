package dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;


import dev.webfx.extras.controlfactory.MaterialFactoryMixin;
import dev.webfx.extras.i18n.I18n;
import dev.webfx.extras.i18n.controls.I18nControls;
import dev.webfx.extras.async.AsyncSpinner;
import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.extras.util.control.Controls;
import dev.webfx.extras.util.control.HtmlInputAutocomplete;
import dev.webfx.extras.util.layout.Layouts;
import dev.webfx.extras.validation.ValidationSupport;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.windowlocation.WindowLocation;
import dev.webfx.stack.authn.AuthenticateWithUsernamePasswordCredentials;
import dev.webfx.stack.authn.AuthenticateWithVerificationCodeCredentials;
import dev.webfx.stack.authn.AuthenticationRequest;
import dev.webfx.stack.authn.InitiateAccountCreationCredentials;
import dev.webfx.stack.authn.SendMagicLinkCredentials;
import dev.webfx.stack.authn.login.ui.FXLoginContext;
import dev.webfx.stack.authn.login.ui.spi.impl.gateway.UiLoginPortalCallback;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;

import java.util.function.Consumer;

/**
 * @author David Hello
 */
public final class UILoginView implements MaterialFactoryMixin {

    private static final String CHECKMARK_PATH = "M12 2a10 10 0 1010 10A10 10 0 0012 2zm0 18a8 8 0 118-8 8 8 0 01-8 8zM14.7 8.4l-3.8 5-1.6-2.1a1 1 0 00-1.6 1.2l2.4 3.1a1 1 0 00.8.4 1 1 0 00.8-.4l4.6-6a1 1 0 10-1.6-1.2z";
    private static final String EYE_OPEN_PATH = "M11 1.5C6 1.5 1.7 4.6 0 9c1.7 4.4 6 7.5 11 7.5s9.3-3.1 11-7.5c-1.7-4.4-6-7.5-11-7.5zM11 14c-2.8 0-5-2.2-5-5s2.2-5 5-5 5 2.2 5 5-2.2 5-5 5zm0-8c-1.7 0-3 1.3-3 3s1.3 3 3 3 3-1.3 3-3-1.3-3-3-3z";
    private static final String EYE_CLOSE_PATH = "M11 4c2.8 0 5 2.2 5 5 0 .7-.1 1.3-.4 1.8l2.9 2.9c1.5-1.3 2.7-2.9 3.4-4.8-1.7-4.4-6-7.5-11-7.5-1.4 0-2.7.3-4 .7l2.2 2.2C9.7 4.1 10.3 4 11 4zM1 1.3l2.3 2.3.5.5C2.1 5.3.8 7 0 9c1.7 4.4 6 7.5 11 7.5 1.6 0 3-.3 4.4-.8l.4.4L18.7 19 20 17.7 2.3 0 1 1.3zM6.5 6.8l1.6 1.6c-.1.2-.1.4-.1.7 0 1.7 1.3 3 3 3 .2 0 .4 0 .7-.1l1.6 1.6c-.7.3-1.4.5-2.2.5-2.8 0-5-2.2-5-5 0-.8.2-1.5.5-2.2zm4.3-.8 3.1 3.1 0-.2c0-1.7-1.3-3-3-3l-.2 0z";

    private final SVGPath eyeIconOpen = new SVGPath();
    private final SVGPath eyeIconClose = new SVGPath(); {
        eyeIconOpen.setContent(EYE_OPEN_PATH);
        eyeIconClose.setContent(EYE_CLOSE_PATH);
    }

    private Label loginTitleLabel;
    private Label mainMessageLabel;
    private TextField emailTextField;
    private PasswordField passwordField;
    private TextField visiblePasswordField;
    private MonoPane eyeButtonMonoPane;
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
    private boolean isPasswordVisible = false;
    private boolean createAccountLinkForceHidden = false;
    private int lastAnchor, lastCaretPosition;

    // Verification code UI components
    private HBox verificationCodeContainer;
    private TextField[] codeDigitFields;
    private Label codeErrorLabel;
    private Hyperlink resendCodeHyperlink;
    private Label orClickLinkLabel;
    private String currentEmail;
    private UiLoginPortalCallback currentCallback;

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

        // Initialize the eye button after everything is set up and added to the scene graph
        Platform.runLater(this::initializeEyeButton);
    }

    private void initialiseMainVBox(VBox container) {
        loginTitleLabel = Bootstrap.h2Primary(I18nControls.newLabel(PasswordI18nKeys.Recovery));
        loginTitleLabel.setPadding(new Insets(10, 0, 0, 0));

        mainMessageLabel = Bootstrap.textSuccess(new Label("Success Message"));
        mainMessageLabel.setPadding(new Insets(60, 0, 0, 0));
        mainMessageLabel.setTextAlignment(TextAlignment.CENTER);
        mainMessageLabel.setWrapText(true);
        mainMessageLabel.setGraphicTextGap(15);
        hideMainMessage();

        emailTextField = newMaterialTextField(PasswordI18nKeys.Email);
        emailTextField.setPrefWidth(370);
        VBox.setMargin(emailTextField, new Insets(40, 0, 0, 0));
        Controls.setHtmlInputTypeAndAutocompleteToEmail(emailTextField);

        // Create password field container with eye icon
        passwordField = newMaterialPasswordField(PasswordI18nKeys.Password);
        Controls.setHtmlInputAutocomplete(passwordField, HtmlInputAutocomplete.CURRENT_PASSWORD);
        passwordField.setPrefWidth(340);

        // Create the visible text field (hidden by default)
        visiblePasswordField = newMaterialTextField(PasswordI18nKeys.Password);
        Controls.setHtmlInputAutocomplete(visiblePasswordField, HtmlInputAutocomplete.CURRENT_PASSWORD);
        visiblePasswordField.setPrefWidth(340);
        Layouts.setManagedAndVisibleProperties(visiblePasswordField, false);

        // Create the eye icon button
        eyeButtonMonoPane = new MonoPane();
        eyeButtonMonoPane.setBackground(Background.EMPTY);
        Layouts.setFixedSize(eyeButtonMonoPane, 22, 19);
        eyeButtonMonoPane.setCursor(Cursor.HAND);

        // Set eye icon SVG
        eyeButtonMonoPane.setContent(eyeIconOpen);

        HBox passwordContainer = new HBox(passwordField, visiblePasswordField, eyeButtonMonoPane);
        passwordContainer.setAlignment(Pos.CENTER_LEFT);

        infoMessageForPasswordFieldLabel = Bootstrap.small(I18nControls.newLabel(PasswordI18nKeys.CaseSensitive));

        passwordFieldAndMessageVbox = new VBox(10,
            passwordContainer,
            infoMessageForPasswordFieldLabel
        );
        VBox.setMargin(passwordContainer, new Insets(15, 0, 0, 0));

        emailAndPasswordContainer = new VBox(emailTextField, passwordFieldAndMessageVbox);
        emailAndPasswordContainer.setAlignment(Pos.CENTER);
        int vBoxHeight = 150;
        emailAndPasswordContainer.setMinHeight(vBoxHeight);
        emailAndPasswordContainer.setMaxHeight(vBoxHeight);

        forgetRememberPasswordHyperlink = Bootstrap.textSecondary(I18nControls.newHyperlink(PasswordI18nKeys.GoToLogin));
        VBox.setMargin(forgetRememberPasswordHyperlink, new Insets(40, 0, 0, 0));

        createAccountHyperlink = I18nControls.newHyperlink(PasswordI18nKeys.CreateAccount);
        if (createAccountEmailConsumer == null) {
            hideCreateAccountHyperlink();
        }
        VBox.setMargin(createAccountHyperlink, new Insets(20, 0, 0, 0));

        actionButton = Bootstrap.largePrimaryButton(I18nControls.newButton(PasswordI18nKeys.Continue));
        VBox.setMargin(actionButton, new Insets(30, 0, 0, 0));

        mainVBox = new VBox(
            loginTitleLabel,
            mainMessageLabel,
            emailAndPasswordContainer,
            forgetRememberPasswordHyperlink,
            createAccountHyperlink,
            actionButton
        );
        mainVBox.setMinWidth(container.getMinWidth());
        //mainVBox.getStyleClass().add("login");
        mainVBox.setAlignment(Pos.TOP_CENTER);
    }

    private void initializeEyeButton() {
        // Create a unified method to memorize the current anchor and caret position, so we can restore it later when
        // switching from the normal password to the visible password field (and vice versa).
        Runnable updateCaretPosition = () -> {
            TextField activeTextField = passwordField.isVisible() ? passwordField : visiblePasswordField;
            if (activeTextField.isFocused()) {
                lastAnchor = activeTextField.getAnchor();
                lastCaretPosition = activeTextField.getCaretPosition();
            }
        };

        // Track anchor and caret position changes more comprehensively
        FXProperties.runOnPropertiesChange(updateCaretPosition,
            passwordField.anchorProperty(), visiblePasswordField.anchorProperty(),
            passwordField.caretPositionProperty(), visiblePasswordField.caretPositionProperty(),
            passwordField.focusedProperty(), visiblePasswordField.focusedProperty(),
            passwordField.textProperty(), visiblePasswordField.textProperty()
        );

        // Keep existing mouse and key event handlers as backup
        passwordField.setOnMousePressed(event -> Platform.runLater(updateCaretPosition));
        visiblePasswordField.setOnMousePressed(event -> Platform.runLater(updateCaretPosition));

        passwordField.setOnKeyReleased(event -> updateCaretPosition.run());
        visiblePasswordField.setOnKeyReleased(event -> updateCaretPosition.run());

        // Eye button toggle event handler - simplified to just click
        eyeButtonMonoPane.setOnMouseClicked(event -> togglePasswordVisibility());
    }

    private void togglePasswordVisibility() {
        showPassword(!isPasswordVisible);
    }

    private void showPassword(boolean visible) {
        isPasswordVisible = visible;

        // Update the visible field with the current password text
        TextField leavingTextField = visible ? passwordField : visiblePasswordField;
        TextField enteringTextField = visible ? visiblePasswordField : passwordField;
        enteringTextField.setText(leavingTextField.getText());

        // Switch visibility
        Layouts.setManagedAndVisibleProperties(passwordField, !visible);
        Layouts.setManagedAndVisibleProperties(visiblePasswordField, visible);

        int anchor = lastAnchor, caretPosition = lastCaretPosition;

        // Request focus and set caret position in the next UI cycle
        Platform.runLater(() -> {
            enteringTextField.requestFocus();

            // Set the caret position after focus is established
            Platform.runLater(() -> {
                enteringTextField.selectRange(anchor, caretPosition);
            });
        });

        eyeButtonMonoPane.setContent(visible ? eyeIconClose : eyeIconOpen);
        // Compensating slight vertical translation when switching eye icon
        eyeButtonMonoPane.setTranslateY(visible ? 0.5 : 0);
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
                Object credentials = new InitiateAccountCreationCredentials(emailTextField.getText().trim().toLowerCase(), WindowLocation.getOrigin(), WindowLocation.getPath(), I18n.getLanguage(), false, FXLoginContext.getLoginContext());
                AsyncSpinner.displayButtonSpinner(actionButton);
                new AuthenticationRequest()
                    .setUserCredentials(credentials)
                    .executeAsync()
                    .inUiThread()
                    .onComplete(ar -> AsyncSpinner.hideButtonSpinner(actionButton))
                    .onFailure(failure -> {
                        callback.notifyUserLoginFailed(failure);
                        setInfoMessageForPasswordFieldLabel(PasswordI18nKeys.ErrorOccurred, Bootstrap.TEXT_DANGER);
                        showMessageForPasswordField();
                    })
                    .onSuccess(ignored -> {
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
                    });
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
        actionButton.setDisable(false);
        showCreateAccountHyperlink();
        createAccountHyperlink.setOnAction(e-> transformPaneToCreateAccount(callback));
        actionButton.setOnAction(e->{
            if (validateForm()) {
                transformPaneToLoginAndPasswordState(callback);
            }
        });

        // Reset verification code UI state
        hideVerificationCodeUI();
        clearCodeFields();
        currentEmail = null;

        // Re-enable and show email field
        emailTextField.setDisable(false);
        showEmailField();

        moveActionButtonUnderEmail();

        // Reset password visibility to the hidden state when returning to the initial state
        if (isPasswordVisible) {
            showPassword(false);
        }
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
                Object credentials = new AuthenticateWithUsernamePasswordCredentials(emailTextField.getText().trim().toLowerCase(), getPassword().trim());
                AsyncSpinner.displayButtonSpinner(actionButton);
                new AuthenticationRequest()
                    .setUserCredentials(credentials)
                    .executeAsync()
                    .inUiThread()
                    .onComplete(ar -> AsyncSpinner.hideButtonSpinner(actionButton))
                    .onFailure(failure -> {
                        callback.notifyUserLoginFailed(failure);
                        setInfoMessageForPasswordFieldLabel(PasswordI18nKeys.IncorrectLoginOrPassword, Bootstrap.TEXT_DANGER);
                        showMessageForPasswordField();
                    })
                    .onSuccess(ignored -> UiScheduler.scheduleDelay(1000, this::clearPassword));
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
                Object credentials = new SendMagicLinkCredentials(emailTextField.getText().trim().toLowerCase(), WindowLocation.getOrigin(), WindowLocation.getPath(), I18n.getLanguage(), false, FXLoginContext.getLoginContext());
                AsyncSpinner.displayButtonSpinner(actionButton);
                new AuthenticationRequest()
                    .setUserCredentials(credentials)
                    .executeAsync()
                    .inUiThread()
                    .onComplete(ar -> AsyncSpinner.hideButtonSpinner(actionButton))
                    .onFailure(failure -> {
                        callback.notifyUserLoginFailed(failure);
                        setInfoMessageForPasswordFieldLabel(PasswordI18nKeys.IncorrectLoginOrPassword, Bootstrap.TEXT_DANGER);
                        showMessageForPasswordField();
                    })
                    .onSuccess(ignored -> {
                        currentEmail = emailTextField.getText().trim().toLowerCase();
                        transformPaneToVerificationCodeEntryState(callback);
                    });
            }
        });
    }

    public void hideCreateAccountHyperlink() {
        createAccountLinkForceHidden = true;
        Layouts.setManagedAndVisibleProperties(createAccountHyperlink, false);
    }

    public void showCreateAccountHyperlink() {
        if (!createAccountLinkForceHidden) {
            Layouts.setManagedAndVisibleProperties(createAccountHyperlink, true);
        }
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
        Layouts.setManagedAndVisibleProperties(emailTextField, false);
    }

    public void showEmailField() {
        Layouts.setManagedAndVisibleProperties(emailTextField, true);
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
        Layouts.setManagedAndVisibleProperties(mainMessageLabel, true);
    }

    public void hideMainMessage() {
        Layouts.setManagedAndVisibleProperties(mainMessageLabel, false);
    }

    public void showPasswordField() {
        Layouts.setManagedAndVisibleProperties(getPasswordFieldAndMessageVbox(), true);
    }

    public void hidePasswordField() {
        Layouts.setManagedAndVisibleProperties(getPasswordFieldAndMessageVbox(), false);
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

    /**
     * Gets the current password text from the active password field (visible or hidden).
     * @return the password text
     */
    public String getPassword() {
        return isPasswordVisible ? visiblePasswordField.getText() : passwordField.getText();
    }

    /**
     * Clears both password fields.
     */
    public void clearPassword() {
        passwordField.setText("");
        visiblePasswordField.setText("");
    }

    // ==================== Verification Code UI Methods ====================

    private void createVerificationCodeUI() {
        codeDigitFields = new TextField[6];
        verificationCodeContainer = new HBox(10);
        verificationCodeContainer.setAlignment(Pos.CENTER);

        for (int i = 0; i < 6; i++) {
            TextField field = createCodeDigitField(i);
            codeDigitFields[i] = field;
            verificationCodeContainer.getChildren().add(field);
        }

        codeErrorLabel = Bootstrap.small(new Label());
        codeErrorLabel.getStyleClass().add(Bootstrap.TEXT_DANGER);
        codeErrorLabel.setWrapText(true);
        codeErrorLabel.setTextAlignment(TextAlignment.CENTER);
        hideCodeError();

        orClickLinkLabel = Bootstrap.textSecondary(I18nControls.newLabel(PasswordI18nKeys.OrClickLinkInEmail));
        orClickLinkLabel.setWrapText(true);
        orClickLinkLabel.setTextAlignment(TextAlignment.CENTER);
        VBox.setMargin(orClickLinkLabel, new Insets(20, 0, 10, 0));

        resendCodeHyperlink = Bootstrap.textSecondary(I18nControls.newHyperlink(PasswordI18nKeys.ResendCode));
    }

    private TextField createCodeDigitField(int index) {
        TextField field = new TextField();
        field.setPrefWidth(45);
        field.setMinWidth(45);
        field.setMaxWidth(45);
        field.setPrefHeight(55);
        field.setMinHeight(55);
        field.setMaxHeight(55);
        field.setAlignment(Pos.CENTER);
        field.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 24px; -fx-font-weight: bold;");
        setupCodeDigitField(field, index);
        return field;
    }

    private void setupCodeDigitField(TextField field, int index) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            // Filter non-digits
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
                return;
            }
            // Handle paste of full code (6+ digits pasted into a field)
            if (newVal.length() >= 6) {
                distributeCodeToFields(newVal.substring(0, 6));
                // Auto-validate after paste
                Platform.runLater(this::autoValidateIfComplete);
                return;
            }
            // Limit to 1 digit
            if (newVal.length() > 1) {
                field.setText(newVal.substring(0, 1));
                return;
            }
            // Auto-advance to next field when digit entered
            if (newVal.length() == 1 && index < 5) {
                codeDigitFields[index + 1].requestFocus();
            }
            // Clear error on input
            hideCodeError();
            // Auto-validate when all 6 digits are entered
            if (newVal.length() == 1) {
                Platform.runLater(this::autoValidateIfComplete);
            }
        });

        // Handle backspace navigation
        field.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.BACK_SPACE && field.getText().isEmpty() && index > 0) {
                codeDigitFields[index - 1].requestFocus();
                codeDigitFields[index - 1].clear();
            }
        });
    }

    private void autoValidateIfComplete() {
        String code = getEnteredCode();
        if (code.length() == 6 && currentCallback != null) {
            onVerifyCode(currentCallback);
        }
    }

    private void distributeCodeToFields(String code) {
        for (int i = 0; i < 6 && i < code.length(); i++) {
            codeDigitFields[i].setText(String.valueOf(code.charAt(i)));
        }
        codeDigitFields[5].requestFocus();
        codeDigitFields[5].positionCaret(1);
    }

    public void transformPaneToVerificationCodeEntryState(UiLoginPortalCallback callback) {
        currentCallback = callback;

        // Create verification code UI components if not already created
        if (verificationCodeContainer == null) {
            createVerificationCodeUI();
        }

        // Set title
        I18nControls.bindI18nProperties(loginTitleLabel, PasswordI18nKeys.VerifyYourEmail);

        // Set main message with email parameter
        I18nControls.bindI18nProperties(mainMessageLabel, PasswordI18nKeys.CodeSentToEmail, currentEmail);
        mainMessageLabel.getStyleClass().setAll(Bootstrap.TEXT_SECONDARY);
        mainMessageLabel.setPadding(new Insets(20, 15, 20, 15));
        showMainMessage();

        // Hide email/password fields
        hideEmailField();
        hidePasswordField();
        hideMessageForPasswordField();

        // Configure action button for verification
        I18nControls.bindI18nProperties(actionButton, PasswordI18nKeys.Verify);
        actionButton.setDisable(false);
        actionButton.setOnAction(e -> onVerifyCode(callback));
        hideGraphicFromActionButton();

        // Configure resend hyperlink
        resendCodeHyperlink.setOnAction(e -> onResendCode(callback));

        // Hide other elements
        hideForgetPasswordHyperlink();
        hideCreateAccountHyperlink();

        // Clear any previous code entries
        clearCodeFields();
        hideCodeError();

        // Show and rearrange mainVBox layout for verification code entry
        showVerificationCodeUI();
        rearrangeForVerificationCodeEntry();

        // Focus first code field
        Platform.runLater(() -> codeDigitFields[0].requestFocus());
    }

    private void rearrangeForVerificationCodeEntry() {
        mainVBox.getChildren().setAll(
            loginTitleLabel,
            mainMessageLabel,
            verificationCodeContainer,
            codeErrorLabel,
            actionButton,
            orClickLinkLabel,
            resendCodeHyperlink
        );
    }

    private void onVerifyCode(UiLoginPortalCallback callback) {
        String code = getEnteredCode();

        if (code.length() != 6) {
            showCodeError(PasswordI18nKeys.InvalidVerificationCode);
            return;
        }

        AsyncSpinner.displayButtonSpinner(actionButton);
        new AuthenticationRequest()
            .setUserCredentials(new AuthenticateWithVerificationCodeCredentials(code))
            .executeAsync()
            .inUiThread()
            .onComplete(ar -> AsyncSpinner.hideButtonSpinner(actionButton))
            .onFailure(failure -> {
                callback.notifyUserLoginFailed(failure);
                showCodeError(PasswordI18nKeys.InvalidOrExpiredCode);
                clearCodeFields();
                Platform.runLater(() -> codeDigitFields[0].requestFocus());
            });
        // Success is handled by FXUserId listener in PasswordUiLoginGateway
    }

    private void onResendCode(UiLoginPortalCallback callback) {
        Object credentials = new SendMagicLinkCredentials(
            currentEmail,
            WindowLocation.getOrigin(),
            WindowLocation.getPath(),
            I18n.getLanguage(),
            false,
            FXLoginContext.getLoginContext()
        );

        AsyncSpinner.displayButtonSpinner(actionButton);
        new AuthenticationRequest()
            .setUserCredentials(credentials)
            .executeAsync()
            .inUiThread()
            .onComplete(ar -> AsyncSpinner.hideButtonSpinner(actionButton))
            .onFailure(failure -> {
                callback.notifyUserLoginFailed(failure);
                showCodeError(PasswordI18nKeys.ErrorOccurred);
            })
            .onSuccess(ignored -> {
                I18nControls.bindI18nProperties(mainMessageLabel, PasswordI18nKeys.CodeResent);
                mainMessageLabel.getStyleClass().setAll(Bootstrap.TEXT_SUCCESS);
                // Reset message after a delay
                UiScheduler.scheduleDelay(3000, () -> {
                    I18nControls.bindI18nProperties(mainMessageLabel, PasswordI18nKeys.CodeSentToEmail, currentEmail);
                    mainMessageLabel.getStyleClass().setAll(Bootstrap.TEXT_SECONDARY);
                });
            });
    }

    private String getEnteredCode() {
        StringBuilder code = new StringBuilder();
        for (TextField field : codeDigitFields) {
            code.append(field.getText());
        }
        return code.toString();
    }

    private void clearCodeFields() {
        if (codeDigitFields != null) {
            for (TextField field : codeDigitFields) {
                field.clear();
            }
        }
    }

    private void showCodeError(Object i18nKey) {
        I18nControls.bindI18nProperties(codeErrorLabel, i18nKey);
        codeErrorLabel.setVisible(true);
        codeErrorLabel.setManaged(true);
    }

    private void hideCodeError() {
        if (codeErrorLabel != null) {
            codeErrorLabel.setVisible(false);
            codeErrorLabel.setManaged(false);
        }
    }

    private void hideVerificationCodeUI() {
        if (verificationCodeContainer != null) {
            Layouts.setManagedAndVisibleProperties(verificationCodeContainer, false);
            Layouts.setManagedAndVisibleProperties(codeErrorLabel, false);
            Layouts.setManagedAndVisibleProperties(orClickLinkLabel, false);
            Layouts.setManagedAndVisibleProperties(resendCodeHyperlink, false);
        }
    }

    private void showVerificationCodeUI() {
        if (verificationCodeContainer != null) {
            Layouts.setManagedAndVisibleProperties(verificationCodeContainer, true);
            Layouts.setManagedAndVisibleProperties(orClickLinkLabel, true);
            Layouts.setManagedAndVisibleProperties(resendCodeHyperlink, true);
            // codeErrorLabel visibility is managed separately by showCodeError/hideCodeError
        }
    }
}