package dev.webfx.stack.orm.entity.controls.entity.selector;

import dev.webfx.platform.util.function.Callable;
import dev.webfx.stack.ui.controls.button.ButtonFactoryMixin;
import javafx.scene.layout.Pane;

/**
 * @author Bruno Salmon
 */
public final class ButtonSelectorParameters {

    private Callable<Pane> dropParentGetter;
    private Pane dropParent;
    private Callable<Pane> dialogParentGetter;
    private Pane dialogParent;
    private ButtonFactoryMixin buttonFactory;

    public ButtonSelectorParameters() {
    }

    public ButtonSelectorParameters(Callable<Pane> dropParentGetter, Pane dropParent, ButtonFactoryMixin buttonFactory) {
        this(dropParentGetter, dropParent, null, null, buttonFactory);
    }

    public ButtonSelectorParameters(Callable<Pane> dropParentGetter, Pane dropParent, Callable<Pane> dialogParentGetter, Pane dialogParent, ButtonFactoryMixin buttonFactory) {
        this.dropParentGetter = dropParentGetter;
        this.dropParent = dropParent;
        this.dialogParentGetter = dialogParentGetter;
        this.dialogParent = dialogParent;
        this.buttonFactory = buttonFactory;
    }

    public ButtonSelectorParameters setDropParentGetter(Callable<Pane> dropParentGetter) {
        this.dropParentGetter = dropParentGetter;
        return this;
    }

    public ButtonSelectorParameters setDropParent(Pane dropParent) {
        this.dropParent = dropParent;
        return this;
    }

    public ButtonSelectorParameters setDialogParentGetter(Callable<Pane> dialogParentGetter) {
        this.dialogParentGetter = dialogParentGetter;
        return this;
    }

    public ButtonSelectorParameters setDialogParent(Pane dialogParent) {
        this.dialogParent = dialogParent;
        return this;
    }

    public ButtonSelectorParameters setButtonFactory(ButtonFactoryMixin buttonFactory) {
        this.buttonFactory = buttonFactory;
        return this;
    }

    public void checkValid() {
        if (dropParentGetter == null && dropParent == null && dialogParentGetter == null && dialogParent == null)
            throw new IllegalArgumentException("No parent has been set for the button selector");
        if (dialogParentGetter == null && dialogParent == null) {
            dialogParentGetter = dropParentGetter;
            dialogParent = dropParent;
        } else if (dropParentGetter == null && dropParent == null) {
            dropParentGetter = dialogParentGetter;
            dropParent = dialogParent;
        }
        if (buttonFactory == null)
            throw new IllegalArgumentException("No button factory has been set for the button selector");
    }

    public Pane getDropParent() {
        return dropParentGetter != null ? dropParentGetter.call() : dropParent;
    }

    public Pane getDialogParent() {
        return dialogParentGetter != null ? dialogParentGetter.call() : dialogParent;
    }

    public ButtonFactoryMixin getButtonFactory() {
        return buttonFactory;
    }
}
