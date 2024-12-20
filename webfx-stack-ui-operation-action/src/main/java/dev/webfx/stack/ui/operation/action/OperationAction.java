package dev.webfx.stack.ui.operation.action;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.function.Factory;
import dev.webfx.stack.ui.action.impl.WritableAction;
import dev.webfx.stack.ui.exceptions.UserCancellationException;
import dev.webfx.stack.ui.operation.OperationUtil;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class OperationAction<Rq, Rs> extends WritableAction {

    private static Function<Object, Node> actionExecutingIconFactory;
    private static BiFunction<Object, Throwable, Node> actionExecutedIconFactory;

    private final Function<ActionEvent, Rq> operationRequestFactory;
    private OperationActionRegistry operationActionRegistry = OperationActionRegistry.getInstance();
    private boolean executing;

    public OperationAction(Factory<Rq> operationRequestFactory, AsyncFunction<Rq, Rs> topOperationExecutor, ObservableValue<?>... graphicalDependencyProperties) {
        this(actionEvent -> operationRequestFactory.create(), topOperationExecutor, graphicalDependencyProperties);
    }

    public OperationAction(Function<ActionEvent, Rq> operationRequestFactory, AsyncFunction<Rq, Rs> topOperationExecutor, ObservableValue<?>... graphicalDependencies) {
        this(new OperationAction[1], operationRequestFactory, topOperationExecutor, graphicalDependencies);
    }

    private OperationAction(OperationAction<Rq, Rs>[] me, Function<ActionEvent, Rq> operationRequestFactory, AsyncFunction<Rq, Rs> topOperationExecutor, ObservableValue<?>... graphicalDependencies) {
        super(actionEvent -> {
            Rq operationRequest = operationRequestFactory.apply(actionEvent);
            Console.log("Executing " + operationRequest);
            long t0 = System.currentTimeMillis();
            me[0].startShowingActionAsExecuting(operationRequest);
            OperationUtil.executeOperation(operationRequest, topOperationExecutor)
                    .onComplete(ar -> {
                        if (ar.succeeded()) {
                            Console.log("Executed " + operationRequest + " in " + (System.currentTimeMillis() - t0) + "ms");
                        } else {
                            if (ar.cause() instanceof UserCancellationException) {
                                Console.log("User cancelled execution of " + operationRequest);
                            } else {
                                Console.log("An error occurred while executing " + operationRequest, ar.cause());
                            }
                        }
                        UiScheduler.runInUiThread(() -> me[0].stopShowingActionAsExecuting(operationRequest, ar.cause()));
                    });
        });
        me[0] = this;
        this.operationRequestFactory = operationRequestFactory;
        OperationActionRegistry registry = getOperationActionRegistry();
        FXProperties.runNowAndOnPropertiesChange(() ->
            registry.bindOperationActionGraphicalProperties(this)
        , graphicalDependencies); // Also updating the graphical properties when graphical dependencies change
    }

    public OperationActionRegistry getOperationActionRegistry() {
        return operationActionRegistry;
    }

    public void setOperationActionRegistry(OperationActionRegistry operationActionRegistry) {
        this.operationActionRegistry = operationActionRegistry;
    }

    public Function<ActionEvent, Rq> getOperationRequestFactory() {
        return operationRequestFactory;
    }

    private void startShowingActionAsExecuting(Object operationRequest) {
        executing = true;
        // Disabling this action during its execution
        FXProperties.setEvenIfBound(writableDisabledProperty(), true);
        // If in addition an icon has been provided to graphically indicate the execution is in progress,
        if (actionExecutingIconFactory != null) { // we apply it to the graphic property
            Node executingIcon = actionExecutingIconFactory.apply(operationRequest);
            if (executingIcon != null) // For some operations such as routing operation, there is no executing icon
                FXProperties.setEvenIfBound(writableGraphicFactoryProperty(), () -> executingIcon);
        }
    }

    private void stopShowingActionAsExecuting(Object operationRequest, Throwable exception) {
        executing = false;
        // Enabling the action again after its execution (by reestablishing the binding). This also reestablishes the
        // original action icon if the executing icon had been applied.
        getOperationActionRegistry().bindOperationActionGraphicalProperties(this);
        // If in addition an icon has been provided to graphically indicate the execution has ended,
        if (actionExecutedIconFactory != null) { // we apply it to the graphic property for 2s
            Node executedIcon = actionExecutedIconFactory.apply(operationRequest, exception);
            if (executedIcon != null) // For some operations such as routing operation, there is no executed icon
                FXProperties.setEvenIfBound(writableGraphicFactoryProperty(), () -> executedIcon);
            UiScheduler.scheduleDelay(2000, () -> {
                // After 2 seconds, we reestablish the original action icon, unless it's already executing again
                if (!executing) { // if executing again, we keep the possible executing icon instead
                    getOperationActionRegistry().bindOperationActionGraphicalProperties(this);
                }
            });
        }
    }

    public static void setActionExecutingIconFactory(Function<Object, Node> actionExecutingIconFactory) {
        OperationAction.actionExecutingIconFactory = actionExecutingIconFactory;
    }

    public static void setActionExecutedIconFactory(BiFunction<Object, Throwable, Node> actionExecutedIconFactory) {
        OperationAction.actionExecutedIconFactory = actionExecutedIconFactory;
    }
}
