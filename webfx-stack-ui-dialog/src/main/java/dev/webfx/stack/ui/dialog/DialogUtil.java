package dev.webfx.stack.ui.dialog;

import dev.webfx.extras.util.control.ControlUtil;
import dev.webfx.extras.util.layout.LayoutUtil;
import dev.webfx.extras.util.scene.SceneUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.kit.util.properties.Unregisterable;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.Booleans;
import dev.webfx.platform.util.collection.Collections;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class DialogUtil {

    private final static Property<Background> dialogBackgroundProperty = new SimpleObjectProperty<>();
    public static Property<Background> dialogBackgroundProperty() {
        return dialogBackgroundProperty;
    }

    private final static Property<Border> dialogBorderProperty = new SimpleObjectProperty<>();
    public static Property<Border> dialogBorderProperty() {
        return dialogBorderProperty;
    }

    public static DialogCallback showModalNodeInGoldLayout(Region modalNode, Pane parent) {
        return showModalNodeInGoldLayout(modalNode, parent, 0, 0);
    }

    public static DialogCallback showModalNodeInGoldLayout(Region modalNode, Pane parent, double percentageWidth, double percentageHeight) {
        Insets padding = modalNode.getPadding();
        return showModalNode(LayoutUtil.createGoldLayout(decorate(modalNode), percentageWidth, percentageHeight), parent)
            .addCloseHook(() -> modalNode.setPadding(padding));
    }

    public static DialogCallback showModalNode(Region modalNode, Pane parent) {
        DialogCallback dialogCallback = createDialogCallback(LayoutUtil.setMaxSizeToInfinite(modalNode), parent);
        setUpModalNodeResizeRelocate(modalNode, parent, dialogCallback);
        return dialogCallback;
    }

    private static void setUpModalNodeResizeRelocate(Region modalNode, Pane parent, DialogCallback dialogCallback) {
        SceneUtil.onSceneReady(parent, scene -> {
            Unregisterable modalLayout = FXProperties.runNowAndOnPropertiesChange(() -> {
                        Point2D parentSceneXY = parent.localToScene(0, 0);
                        double width = Math.min(parent.getWidth(), scene.getWidth() - parentSceneXY.getX());
                        double height = Math.min(parent.getHeight(), scene.getHeight() - parentSceneXY.getY());
                        modalNode.resizeRelocate(0, 0, width, height);
                    }, parent.widthProperty(), parent.heightProperty(), scene.widthProperty(), scene.heightProperty()
            );
            dialogCallback.addCloseHook(modalLayout::unregister);
        });
    }

    public static BorderPane decorate(Node content) {
        // Setting max width/height to pref width/height (otherwise the grid pane takes all space with cells in top left corner)
        if (content instanceof Region)
            LayoutUtil.setMaxSizeToPref(LayoutUtil.createPadding((Region) content, 10));
        BorderPane decorator = new BorderPane(content);
        decorator.backgroundProperty().bind(dialogBackgroundProperty());
        decorator.borderProperty().bind(dialogBorderProperty());
        decorator.setMinHeight(0d);
        return decorator;
    }

    public static DialogCallback showDropUpOrDownDialog(Region dialogNode, Region buttonNode, Pane parent, ObservableValue resizeProperty, boolean up) {
        DialogCallback dialogCallback = createDialogCallback(dialogNode, parent);
        setUpDropDownDialogResizeRelocate(dialogNode, buttonNode, parent, dialogCallback, resizeProperty, up);
        return dialogCallback;
    }

    private static DialogCallback createDialogCallback(Region dialogNode, Pane parent) {
        dialogNode.setManaged(false);
        parent.getChildren().add(dialogNode);
        return new DialogCallback() {
            private final List<Runnable> closeHooks = new ArrayList<>();
            private boolean closed;
            @Override
            public void closeDialog() {
                if (!closed)
                    UiScheduler.runInUiThread(() -> {
                        // Sequence note: we call the hooks before removing the dialog from the UI because some hooks
                        // may be interested in the UI state before closing, like in ButtonSelector where it decides to
                        // restore the focus to the button if the last focus is inside the dialog
                        for (Runnable closeHook: closeHooks)
                            closeHook.run();
                        // Now we can remove the dialog from the UI
                        parent.getChildren().remove(dialogNode); // May clean the scene focus owner if it was inside
                    });
                closed = true;
            }

            @Override
            public boolean isDialogClosed() {
                return closed;
            }

            @Override
            public void showException(Throwable e) {
                e.printStackTrace();
                //UiScheduler.runInUiThread(() -> AlertUtil.showExceptionAlert(e, parent.getScene().getWindow()));
            }

            @Override
            public DialogCallback addCloseHook(Runnable closeHook) {
                closeHooks.add(closeHook);
                return this;
            }
        };
    }

    private static void setUpDropDownDialogResizeRelocate(Region dialogNode, Region buttonNode, Pane parent, DialogCallback dialogCallback, ObservableValue resizeProperty, boolean up) {
        SceneUtil.onSceneReady(buttonNode, scene -> {
            List<ObservableValue> reactingProperties = Collections.listOf(
                    buttonNode.widthProperty(),
                    buttonNode.heightProperty(),
                    resizeProperty);
            for (ScrollPane scrollPane = ControlUtil.findScrollPaneAncestor(buttonNode); scrollPane != null; scrollPane = ControlUtil.findScrollPaneAncestor(scrollPane)) {
                reactingProperties.add(scrollPane.hvalueProperty());
                reactingProperties.add(scrollPane.vvalueProperty());
            }
            setDropDialogUp(dialogNode, up);
            Runnable positionUpdater = () -> {
                Point2D buttonSceneTopLeft = buttonNode.localToScene(0, 0);
                Point2D buttonSceneBottomRight = buttonNode.localToScene(buttonNode.getWidth(), buttonNode.getHeight());
                double dialogPrefWidth = dialogNode.prefWidth(-1);
                double dialogWidth = LayoutUtil.boundedSize(dialogPrefWidth, buttonNode.getWidth(), scene.getWidth() - buttonSceneTopLeft.getX());
                double dialogHeight = dialogNode.prefHeight(dialogWidth);
                boolean dropDialogUp = isDropDialogUp(dialogNode);
                Point2D buttonParentTopLeft = parent.sceneToLocal(buttonSceneTopLeft);
                double dialogX = buttonParentTopLeft.getX();
                double dialogY;
                if (dropDialogUp) {
                    dialogY = buttonSceneTopLeft.getY() - dialogHeight;
                } else {
                    Point2D buttonParentBottomRight = parent.sceneToLocal(buttonSceneBottomRight);
                    dialogY = buttonParentBottomRight.getY();
                }
                if (isDropDialogBounded(dialogNode)) {
                    if (dropDialogUp)
                        dialogY = Math.min(dialogY, parent.getHeight() - dialogHeight);
                    else
                        dialogY = Math.max(dialogY, 0);
                }
                Region.layoutInArea(dialogNode, dialogX, dialogY, dialogWidth, dialogHeight, -1, null, true, false, HPos.LEFT, VPos.TOP, true);
            };
            dialogNode.getProperties().put("webfx-positionUpdater", positionUpdater); // used by updateDropUpOrDownDialogPosition()
            // We automatically close the dialog when we loose the focus (ex: when the user clicks outside the dialog)
            Unregisterable focusLostRegistration =
                    SceneUtil.runOnceFocusIsOutside(dialogNode, false, dialogCallback::closeDialog);
            dialogCallback
                    .addCloseHook(FXProperties.runNowAndOnPropertiesChange(positionUpdater, reactingProperties)::unregister)
                    .addCloseHook(() -> dialogNode.relocate(0, 0))
                    .addCloseHook(focusLostRegistration::unregister);
        });
    }

    public static void setDropDialogUp(Region dialogNode, boolean up) {
        dialogNode.getProperties().put("webfx-dropDialogUp", up);
    }

    public static boolean isDropDialogUp(Region dialogNode) {
        return Booleans.isTrue(dialogNode.getProperties().get("webfx-dropDialogUp"));
    }

    public static void setDropDialogBounded(Region dialogNode, boolean bounded) {
        dialogNode.getProperties().put("webfx-dropDialogBounded", bounded);
    }

    public static boolean isDropDialogBounded(Region dialogNode) {
        return Booleans.isTrue(dialogNode.getProperties().get("webfx-dropDialogBounded"));
    }

    public static void updateDropUpOrDownDialogPosition(Region dialogNode) {
        Object positionUpdater = dialogNode.getProperties().get("webfx-positionUpdater");
        if (positionUpdater instanceof Runnable)
           ((Runnable) positionUpdater).run();
    }

}
