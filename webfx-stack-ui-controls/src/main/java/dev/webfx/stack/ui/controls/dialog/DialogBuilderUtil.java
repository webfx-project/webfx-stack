package dev.webfx.stack.ui.controls.dialog;

import dev.webfx.extras.util.layout.LayoutUtil;
import dev.webfx.stack.ui.dialog.DialogCallback;
import dev.webfx.stack.ui.dialog.DialogUtil;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class DialogBuilderUtil {
    public static DialogCallback showModalNodeInGoldLayout(DialogBuilder dialogBuilder, Pane parent) {
        return showModalNodeInGoldLayout(dialogBuilder, parent, 0, 0);
    }

    public static DialogCallback showModalNodeInGoldLayout(DialogBuilder dialogBuilder, Pane parent, double percentageWidth, double percentageHeight) {
        Region dialog = dialogBuilder.build();
        if (percentageWidth != 0)
            LayoutUtil.setPrefWidthToInfinite(dialog);
        if (percentageHeight != 0)
            LayoutUtil.setPrefHeightToInfinite(dialog);
        DialogCallback dialogCallback = DialogUtil.showModalNodeInGoldLayout(dialog, parent, percentageWidth, percentageHeight);
        dialogBuilder.setDialogCallback(dialogCallback);
        return dialogCallback;
    }

    public static void showDialog(DialogContent dialogContent, Consumer<DialogCallback> okConsumer, Pane parent) {
        DialogUtil.showModalNodeInGoldLayout(dialogContent.build(), parent);
        armDialogContentButtons(dialogContent, okConsumer);
    }

    public static void armDialogContentButtons(DialogContent dialogContent, Consumer<DialogCallback> okConsumer) {
        dialogContent.getCancelButton().setOnAction(event -> dialogContent.getDialogCallback().closeDialog());
        dialogContent.getOkButton().setOnAction(event -> okConsumer.accept(dialogContent.getDialogCallback()));
    }
}
