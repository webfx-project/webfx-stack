package dev.webfx.stack.ui.controls.dialog;

import dev.webfx.stack.ui.controls.button.ButtonFactoryMixin;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public interface DialogBuilder extends ButtonFactoryMixin {

    Region build();

    void setDialogCallback(DialogCallback dialogCallback);

    DialogCallback getDialogCallback();

    default Button newButton(Object i18nKey, Consumer<DialogCallback> dialogAction) {
        return newButton(i18nKey, () -> dialogAction.accept(getDialogCallback()));
    }
}
