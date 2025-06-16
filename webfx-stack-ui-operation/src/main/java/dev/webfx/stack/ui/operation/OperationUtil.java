package dev.webfx.stack.ui.operation;

import dev.webfx.extras.util.control.Controls;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.uischeduler.UiScheduler;
import javafx.scene.Node;
import javafx.scene.control.Labeled;

/**
 * @author Bruno Salmon
 */
public final class OperationUtil {

    private static final String BUTTON_GRAPHIC_MEMO_PROPERTIES_KEY = "webfx-operation-util-graphic";

    public static <Rq, Rs> Future<Rs> executeOperation(Rq operationRequest, AsyncFunction<Rq, Rs> operationExecutor) {
        if (operationExecutor == null && operationRequest instanceof HasOperationExecutor)
            //noinspection unchecked
            operationExecutor = ((HasOperationExecutor<Rq, Rs>) operationRequest).getOperationExecutor();
        if (operationExecutor != null)
            return operationExecutor.apply(operationRequest);
        return Future.failedFuture(new IllegalArgumentException("No executor found for operation request " + operationRequest));
    }

    // Utility methods for managing the button wait mode (is it the right place for these methods?)

    // During execution, the first passed button will show a progress indicator, and all buttons will be disabled.
    // At the end of the execution, all buttons will be enabled again, and the first button graphic will be reset.

    // One issue with these methods is that it unbinds the button graphic property (which is ok during execution) but
    // doesn't reestablish the initial binding at the end (the initial graphic is just reset).

    public static void turnOnButtonsWaitModeDuringExecution(Future<?> future, Labeled... buttons) {
        turnOnButtonsWaitMode(buttons);
        future.onComplete(x -> UiScheduler.runInUiThread(() -> turnOffButtonsWaitMode(buttons)));
    }

    public static void turnOnButtonsWaitMode(Labeled... buttons) {
        setWaitMode(true, buttons);
    }

    public static void turnOffButtonsWaitMode(Labeled... buttons) {
        setWaitMode(false, buttons);
    }

    private static void setWaitMode(boolean on, Labeled... buttons) {
        for (Labeled button : buttons) {
            FXProperties.setIfNotBound(button.disableProperty(), on);
            Node graphic = null;
            if (button == buttons[0]) {
                if (on) {
                    graphic = Controls.createProgressIndicator(20);
                    // Memorizing the previous graphic before changing it
                    button.getProperties().put(BUTTON_GRAPHIC_MEMO_PROPERTIES_KEY, button.getGraphic());
                } else {
                    // Restoring the previous graphic once wait mode is turned off
                    graphic = (Node) button.getProperties().get(BUTTON_GRAPHIC_MEMO_PROPERTIES_KEY);
                }
            }
            FXProperties.setEvenIfBound(button.graphicProperty(), graphic);
        }
    }
}
