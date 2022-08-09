package dev.webfx.stack.ui.operation.action;

import dev.webfx.platform.console.Console;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import dev.webfx.stack.ui.action.impl.WritableAction;
import dev.webfx.stack.ui.operation.OperationUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.util.function.Factory;

import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class OperationAction<Rq, Rs> extends WritableAction {

    private final Function<ActionEvent, Rq> operationRequestFactory;
    private OperationActionRegistry operationActionRegistry = OperationActionRegistry.getInstance();

    public OperationAction(Factory<Rq> operationRequestFactory, AsyncFunction<Rq, Rs> topOperationExecutor, ObservableValue... graphicalDependencyProperties) {
        this(actionEvent -> operationRequestFactory.create(), topOperationExecutor, graphicalDependencyProperties);
    }

    public OperationAction(Function<ActionEvent, Rq> operationRequestFactory, AsyncFunction<Rq, Rs> topOperationExecutor, ObservableValue... graphicalDependencies) {
        super(actionEvent -> {
            Rq operationRequest = operationRequestFactory.apply(actionEvent);
            Console.log("Executing " + operationRequest);
            long t0 = System.currentTimeMillis();
            OperationUtil.executeOperation(operationRequest, topOperationExecutor)
                    .onFailure(cause -> Console.log("Error while executing " + operationRequest, cause))
                    .onSuccess(result -> Console.log("Executed " + operationRequest + " in " + (System.currentTimeMillis() - t0) + "ms"));
        });
        this.operationRequestFactory = operationRequestFactory;
        OperationActionRegistry registry = getOperationActionRegistry();
        registry.bindOperationActionGraphicalProperties(this);
        // Also, if some graphical dependencies are passed, we update the graphical properties when they change
        if (graphicalDependencies.length > 0)
            FXProperties.runNowAndOnPropertiesChange(() -> registry.updateOperationActionGraphicalProperties(this), graphicalDependencies);
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
}
