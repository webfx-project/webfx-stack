package dev.webfx.stack.ui.controls.dialog;

import dev.webfx.extras.styles.bootstrap.Bootstrap;
import dev.webfx.stack.ui.dialog.DialogCallback;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * @author Bruno Salmon
 */
public final class DialogContent implements DialogBuilder {

    private String title;
    private String headerText;
    private String contentText;
    private String primaryButtonText = "Ok";
    private String secondaryButtonText = "Cancel";

    private Node content;
    private Button primaryButton = new Button(); { setPrimaryButton(primaryButton); }
    private Button secondaryButton = new Button(); { setSecondaryButton(secondaryButton); }

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
        primaryButtonText = "Yes";
        secondaryButtonText = "No";
        return this;
    }

    public DialogContent setOk() {
        primaryButtonText = "Ok";
        secondaryButton.setManaged(false);
        return this;
    }

    public Button getPrimaryButton() {
        return primaryButton;
    }

    public DialogContent setPrimaryButton(Button primaryButton) {
        this.primaryButton = Bootstrap.largeSuccessButton(primaryButton);
        primaryButton.setDefaultButton(true);
        return this;
    }

    public Button getSecondaryButton() {
        return secondaryButton;
    }

    public DialogContent setSecondaryButton(Button secondaryButton) {
        this.secondaryButton = Bootstrap.largeSecondaryButton(secondaryButton);
        secondaryButton.setCancelButton(true);
        return this;
    }

    @Override
    public Region build() {
        GridPaneBuilder builder = new GridPaneBuilder();
        if (title != null)
            builder.addTextRow(title);
        if (headerText != null) {
            Label headerLabel = Bootstrap.textSuccess(Bootstrap.h3(newLabel(headerText)));
            headerLabel.setWrapText(true);
            GridPane.setHalignment(headerLabel, HPos.CENTER);
            GridPane.setMargin(headerLabel, new Insets(10));
            builder.addNodeFillingRow(headerLabel);
        }
        if (contentText != null) {
            Label contentLabel = Bootstrap.h4(newLabel(contentText));
            contentLabel.setWrapText(true);
            GridPane.setMargin(contentLabel, new Insets(20));
            builder.addNodeFillingRow(contentLabel);
        }
        if (content != null) {
            builder.addNodeFillingRow(content);
            GridPane.setVgrow(content, Priority.ALWAYS);
        }
        return builder
                .addButtons(primaryButtonText, primaryButton, secondaryButtonText, secondaryButton)
                .build();
    }
}
