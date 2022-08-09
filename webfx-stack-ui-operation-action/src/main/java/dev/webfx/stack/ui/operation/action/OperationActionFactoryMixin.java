package dev.webfx.stack.ui.operation.action;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import dev.webfx.stack.ui.action.Action;
import dev.webfx.stack.ui.action.ActionGroup;
import dev.webfx.stack.ui.action.ActionGroupBuilder;
import dev.webfx.stack.ui.action.impl.SeparatorAction;
import dev.webfx.stack.ui.operation.HasOperationExecutor;
import dev.webfx.stack.ui.operation.OperationUtil;
import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.function.Factory;

import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public interface OperationActionFactoryMixin extends HasOperationExecutor {

    default AsyncFunction getOperationExecutor() {
        return null;
    }

    default <Rq, Rs> Future<Rs> executeOperation(Rq operationRequest) {
        return OperationUtil.executeOperation(operationRequest, getOperationExecutor());
    }

    default OperationActionRegistry getOperationActionRegistry() {
        return OperationActionRegistry.getInstance();
    }

    // OperationAction factory methods

    default <Rq> OperationAction newOperationAction(Factory<Rq> operationRequestFactory, ObservableValue... graphicalDependencies) {
        return newOperationAction(operationRequestFactory, getOperationExecutor(), graphicalDependencies);
    }

    default <Rq, Rs> OperationAction newOperationAction(Factory<Rq> operationRequestFactory, AsyncFunction<Rq, Rs> topOperationExecutor, ObservableValue... graphicalDependencies) {
        return initOperationAction(new OperationAction<>(operationRequestFactory, topOperationExecutor, graphicalDependencies));
    }

    // Same but with an action event passed to the operation request factory

    default <Rq> OperationAction newOperationAction(Function<ActionEvent, Rq> operationRequestFactory, ObservableValue... graphicalDependencies) {
        return newOperationAction(operationRequestFactory, getOperationExecutor(), graphicalDependencies);
    }

    default <Rq, Rs> OperationAction newOperationAction(Function<ActionEvent, Rq> operationRequestFactory, AsyncFunction<Rq, Rs> topOperationExecutor, ObservableValue... graphicalDependencies) {
        return initOperationAction(new OperationAction<>(operationRequestFactory, topOperationExecutor, graphicalDependencies));
    }

    // Action group factory methods

    default Action newSeparatorAction() {
        return new SeparatorAction();
    }

    default ActionGroup newActionGroup(Action... actions) {
        return newActionGroup(null, false, actions);
    }

    default ActionGroup newSeparatorActionGroup(Action... actions) {
        return newActionGroup(null, true, actions);
    }

    default ActionGroup newSeparatorActionGroup(Object i18nKey, Action... actions) {
        return newActionGroup(i18nKey, true, actions);
    }

    default ActionGroup newActionGroup(Object i18nKey, boolean hasSeparators, Action... actions) {
        return new ActionGroupBuilder().setI18nKey(i18nKey).setActions(actions).setHasSeparators(hasSeparators).build();
    }

    default OperationAction initOperationAction(OperationAction operationAction) {
        OperationActionRegistry registry = operationAction.getOperationActionRegistry();
        if (registry == null) {
            registry = getOperationActionRegistry();
            if (registry != null)
                operationAction.setOperationActionRegistry(registry);
        }
        return operationAction;
    }

}
