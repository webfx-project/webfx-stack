package dev.webfx.stack.authn.login.ui.spi.impl.gateway.password;


import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.stack.i18n.controls.I18nControls;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;

public class UILoginView implements dev.webfx.stack.ui.controls.MaterialFactoryMixin {

    private Label loginTitleLabel;
    private Label mainMessageLabel;
    private javafx.scene.control.TextField emailTextField;
    private javafx.scene.control.PasswordField passwordField;
    private Label infoMessageForPasswordFieldLabel;
    private Hyperlink hyperlink;
    private Button actionButton;
    private VBox mainVBox;
    private VBox passwordFieldAndMessageVbox;
    private BorderPane container;

    private static final String CHECKMARCK_PATH = "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z M14.7 8.39l-3.78 5-1.63-2.11a1 1 0 0 0-1.58 1.23l2.43 3.11a1 1 0 0 0 .79.38 1 1 0 0 0 .79-.39l4.57-6a1 1 0 1 0-1.6-1.22z";

    public void initializeComponents() { // Reminder: called only once (rebuild = bad UX) => UI is reacting to parameter changes
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

        mainMessageLabel = Bootstrap.textSuccess(new javafx.scene.control.Label("Success Message"));
        mainMessageLabel.setPadding(new Insets(40, 0, 0, 0));
        mainMessageLabel.setTextAlignment(TextAlignment.CENTER);
        mainMessageLabel.setWrapText(true);
        mainMessageLabel.setGraphicTextGap(15);
        hideMainMessage();

        VBox emailAndPasswordContainer = new VBox();
        emailAndPasswordContainer.setAlignment(Pos.CENTER);
        int vBoxHeight = 150;
        emailAndPasswordContainer.setMinHeight(vBoxHeight);
        emailAndPasswordContainer.setMaxHeight(vBoxHeight);
        emailTextField = newMaterialTextField(PasswordI18nKeys.Email);
        emailTextField.getStyleClass().clear();
        emailTextField.getStyleClass().add("transparent-input");
        VBox.setMargin(emailTextField, new Insets(40, 0, 0, 0));
        emailTextField.setMaxWidth(300);

        passwordFieldAndMessageVbox = new VBox(10);
        passwordField = newMaterialPasswordField(PasswordI18nKeys.Password);
        passwordField.getStyleClass().clear();
        passwordField.getStyleClass().add("transparent-input");
        passwordField.setMaxWidth(300);
        VBox.setMargin(passwordField, new Insets(15, 0, 0, 0));
        passwordFieldAndMessageVbox.setMaxWidth(300);

        infoMessageForPasswordFieldLabel = Bootstrap.small(I18nControls.newLabel(PasswordI18nKeys.CaseSensitive));
        infoMessageForPasswordFieldLabel.setVisible(true);
        passwordFieldAndMessageVbox.getChildren().addAll(passwordField, infoMessageForPasswordFieldLabel);

        emailAndPasswordContainer.getChildren().setAll(emailTextField,passwordFieldAndMessageVbox);
        hyperlink = new Hyperlink();
        I18nControls.bindI18nProperties(hyperlink, PasswordI18nKeys.GoToLogin);
        hyperlink.getStyleClass().setAll(Bootstrap.TEXT_SECONDARY);
        hyperlink.setVisible(true);
        VBox.setMargin(hyperlink, new Insets(40, 0, 0, 0));

        actionButton = Bootstrap.largePrimaryButton(I18nControls.newButton(PasswordI18nKeys.SendLink));
        VBox.setMargin(actionButton, new Insets(30, 0, 0, 0));
        mainVBox.getChildren().addAll(loginTitleLabel, mainMessageLabel, emailAndPasswordContainer, hyperlink, actionButton);
    }


    public void hideMessageForPasswordField() {
        infoMessageForPasswordFieldLabel.setVisible(false);
    }

    public void hideHyperlink() {
        hyperlink.setVisible(false);
    }

    public void showHyperlink() {
        hyperlink.setVisible(true);
    }

    public void setInfoMessageForPasswordFieldLabel(String I18nKey, String bootStrapStyle) {
        I18nControls.bindI18nProperties(infoMessageForPasswordFieldLabel, I18nKey);
        infoMessageForPasswordFieldLabel.getStyleClass().setAll(bootStrapStyle);
    }

    public void setHyperlink(String I18nKey) {
        I18nControls.bindI18nProperties(hyperlink, I18nKey);
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
       // mainMessageLabel.setManaged(true);
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
        svgPath.setContent(CHECKMARCK_PATH);
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
        hideHyperlink();
        hideActionButton();
    }
    public Hyperlink getHyperlink() {
        return hyperlink;
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
