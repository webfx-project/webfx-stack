package dev.webfx.stack.orm.entity.controls.entity.sheet;

import dev.webfx.extras.controlfactory.MaterialFactoryMixin;
import dev.webfx.extras.controlfactory.button.ButtonFactoryMixin;
import dev.webfx.extras.util.dialog.builder.DialogBuilderUtil;
import dev.webfx.extras.util.dialog.builder.DialogContent;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.orm.entity.Entity;
import dev.webfx.stack.orm.entity.UpdateStore;
import dev.webfx.stack.orm.expression.Expression;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

abstract class EntityUpdateDialog<E extends Entity> implements MaterialFactoryMixin, ButtonFactoryMixin {

    Pane dialogParent; // Keeping referenced only for transmission to the EntityRenderingContext
    Node node;
    E entity;
    Entity updateEntity;
    private DialogContent dialogContent;
    private UpdateStore updateStore;

    public E getEntity() {
        return entity;
    }

    public void setEntity(E entity) {
        this.entity = entity;
    }

    public Node getNode() {
        if (node == null) {
            node = buildNode();
            syncUiFromModel();
        }
        return node;
    }

    abstract Node buildNode();

    abstract void syncUiFromModel();

    void updateOkButton() {
        if (dialogContent != null)
            dialogContent.getPrimaryButton().setDisable(!updateStore.hasChanges());
    }

    abstract Expression<? extends Entity> expressionToLoad();

    void showAsDialog(Pane parent) {
        dialogParent = parent;
        entity.onExpressionLoaded(expressionToLoad())
            .inUiThread()
            .onComplete(ar -> {
                if (ar.failed())
                    Console.log(ar.cause());
                else {
                    if (entity.getStore() instanceof UpdateStore) {
                        updateStore = (UpdateStore) entity.getStore();
                        updateEntity = entity;
                    } else {
                        updateStore = UpdateStore.createAbove(entity.getStore());
                        updateEntity = updateStore.updateEntity(entity);
                    }
                    dialogContent = new DialogContent().setContent(getNode());
                    DialogBuilderUtil.showModalNodeInGoldLayout(dialogContent, parent);
                    DialogBuilderUtil.armDialogContentButtons(dialogContent, dialogCallback -> {
                        if (!updateStore.hasChanges())
                            dialogCallback.closeDialog();
                        else {
                            updateStore.submitChanges()
                                .onFailure(dialogCallback::showException)
                                .onSuccess(result -> dialogCallback.closeDialog());
                        }
                    });
                    updateOkButton();
                }
            });
    }

}
