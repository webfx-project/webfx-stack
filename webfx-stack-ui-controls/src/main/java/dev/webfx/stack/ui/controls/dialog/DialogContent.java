package dev.webfx.stack.ui.controls.dialog;

import dev.webfx.stack.ui.dialog.DialogCallback;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * @author Bruno Salmon
 */
public final class DialogContent implements DialogBuilder {

    private String title;
    private String headerText;
    private String contentText;
    private String okText = "Ok";
    private String cancelText = "Cancel";

    private Node content;
    private Button okButton = new Button(); { okButton.setDefaultButton(true); okButton.setPadding(new Insets(5)); } // TODO: replace hardcoded style with CSS
    private Button cancelButton = new Button(); { cancelButton.setCancelButton(true); cancelButton.setPadding(new Insets(5)); } // TODO: replace hardcoded style with CSS

    private DialogCallback dialogCallback;

    public static DialogContent createConfirmationDialog(String headerText, String contentText) {
        return createConfirmationDialog("Confirmation", headerText, contentText);
    }

    public static DialogContent createConfirmationDialog(String title, String headerText, String contentText) {
        return new DialogContent().setTitle(title).setHeaderText(headerText).setContentText(contentText).setYesNo();
    }

    @Override
    public void setDialogCallback(DialogCallback dialogCallback) {
        this.dialogCallback = dialogCallback;
    }

    @Override
    public DialogCallback getDialogCallback() {
        return dialogCallback;
    }

    public DialogContent setTitle(String title) {
        this.title = title;
        return this;
    }

    public DialogContent setHeaderText(String headerText) {
        this.headerText = headerText;
        return this;
    }

    public DialogContent setContentText(String contentText) {
        this.contentText = contentText;
        return this;
    }

    public DialogContent setContent(Node content) {
        this.content = content;
        return this;
    }

    public DialogContent setYesNo() {
        okText = "Yes";
        cancelText = "No";
        return this;
    }

    public Button getOkButton() {
        return okButton;
    }

    public DialogContent setOkButton(Button okButton) {
        this.okButton = okButton;
        return this;
    }

    public Button getCancelButton() {
        return cancelButton;
    }

    public DialogContent setCancelButton(Button cancelButton) {
        this.cancelButton = cancelButton;
        return this;
    }

    @Override
    public Region build() {
        GridPaneBuilder builder = new GridPaneBuilder();
        if (headerText != null)
            builder.addTextRow(headerText);
        if (contentText != null)
            builder.addTextRow(contentText);
        if (content != null) {
            builder.addNodeFillingRow(content);
            GridPane.setVgrow(content, Priority.ALWAYS);
        }
        GridPane gridPane = builder
                .addButtons(okText, okButton, cancelText, cancelButton)
                .build();
        gridPane.setBackground(Background.fill(Color.WHITE)); // TODO: replace hardcoded style with CSS
        return gridPane;
    }
}
