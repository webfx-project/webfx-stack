package dev.webfx.stack.ui.operation.action;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authz.client.factory.AuthorizationUtil;
import dev.webfx.stack.ui.action.Action;
import dev.webfx.stack.ui.action.ActionBinder;
import dev.webfx.stack.ui.action.impl.WritableAction;
import dev.webfx.stack.ui.operation.HasOperationCode;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class is a registry for operation actions, more accurately, for their graphical properties (text, graphic,
 * disabled and visible properties). It allows a complete code separation between the action handling declaration from
 * one side and the graphical properties declaration from the other side.
 * For example, from one side the action handling can be declared typically as follows:
 *      OperationAction myOperationAction = newOperationAction(() -> new MyOperationRequest(myArguments));
 * This code just want an action executing myOperationRequest without telling how this action appear in the user interface.
 * From the other side (another part of the application, usually the initialization code), its graphical properties can
 * be declared and registered typically as follows:
 *      Action myGraphicalAction = newAction(...);
 *      OperationActionRegistry.getInstance().registerOperationGraphicalAction(MyOperationRequest.class, registerOperationGraphicalAction);
 * or if MyOperationRequest implements HasOperationCode:
 *      OperationActionRegistry.getInstance().registerOperationGraphicalAction(myOperationCode, registerOperationGraphicalAction)
 * In this second code, graphical properties can be read from a file or DB listing all operations and bound to I18n. In
 * this case, none of the graphical properties are hardcoded, they are completely dynamic.
 * When both sides have been executed, myOperationAction used in the first code is graphically displayed as myGraphicalAction.
 *
 * @author Bruno Salmon
 */
public final class OperationActionRegistry {

    private static final OperationActionRegistry INSTANCE = new OperationActionRegistry();

    // Holding the actions that have been registered by the application code through registerOperationGraphicalAction().
    // These actions usually hold only the graphical properties, they are not executable (the event handler doesn't do
    // anything). They are used to bind the graphical properties of the executable operation actions - through
    // bindOperationActionGraphicalProperties().
    private final Map<Object /* key = request class or operation code */, Action> registeredGraphicalActions = new HashMap<>();

    // Holding the executable operation actions instantiated by the application code (probably bound to a UI control
    // such as a button) which have been asked to be bound to a registered graphical action, through
    // bindOperationActionGraphicalProperties() but the issue is that this graphical action is not yet registered.
    private Collection<OperationAction> notYetBoundExecutableOperationActions; // to be bound as soon as the graphical action will be registered

    // When the application code wants to be notified when an executable operation action has been bound to its graphical
    // properties, it can get an observable which will transit from null to the operation action at that time.
    private final Map<Object, ObjectProperty<OperationAction>> executableOperationActionNotifyingProperties = new HashMap<>();

    private Scheduled bindScheduled;
    private Consumer<OperationAction> operationActionGraphicalPropertiesUpdater;

    public static OperationActionRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * This method should be used when creating a graphical action for an operation that is not public but requires an
     * authorization. It will return an observable boolean value indicating if the operation is authorized or not
     * (reacting to the user principal change). Needs to be considered when setting up the disabled and visible properties.
     */

    public ObservableBooleanValue authorizedOperationActionProperty(Object operationCode, AsyncFunction<Object, Boolean> authorizationFunction) {
        // Note: it's possible we don't know yet what operation action we are talking about at this stage, because this
        // method can (and usually is) called before the operation action associated with that code is registered
        Function<OperationAction, Object> operationRequestFactory = this::newOperationActionRequest; // Will return null until the operation action with that code is registered
        // We embed the authorization function to handle the special case where the request is null
        AsyncFunction<Object, Boolean> embedAuthorizationFunction = new AsyncFunction<Object, Boolean>() { // Using a lambda expression here causes a wrong GWT code factorization which lead to an application crash! Keeping the Java 7 style solves the problem.
            @Override
            public Future<Boolean> apply(Object request) {
                if (request != null)
                    return authorizationFunction.apply(request);
                // If the request is null, this is because no operation action with that code has yet been registered, so we
                // don't know what operation it is yet, so we return not authorized by default (if this action is shown in a
                // button, the button will be invisible (or at least disabled) until the operation action is registered
                return Future.succeededFuture(false);
            }
        };
        return AuthorizationUtil.authorizedOperationProperty(
                operationRequestFactory
                , embedAuthorizationFunction
                , executableOperationActionNotifyingProperty(operationCode) // reactive property (will change when operation action will be registered, causing a new authorization evaluation)
        );
    }

    public <A> OperationActionRegistry registerOperationGraphicalAction(Class<A> operationRequestClass, Action graphicalAction) {
        return registerOperationGraphicalAction((Object) operationRequestClass, graphicalAction); // because share the same map
    }

    public OperationActionRegistry registerOperationGraphicalAction(Object operationCode, Action graphicalAction) {
        synchronized (registeredGraphicalActions) {
            registeredGraphicalActions.put(operationCode, graphicalAction);
            return checkPendingOperationActionGraphicalBindings();
        }
    }

    private OperationActionRegistry checkPendingOperationActionGraphicalBindings() {
        if (notYetBoundExecutableOperationActions != null && (bindScheduled == null || bindScheduled.isFinished())) {
            bindScheduled = UiScheduler.scheduleDeferred(() -> {
                Collection<OperationAction> operationActionsToBind = notYetBoundExecutableOperationActions;
                notYetBoundExecutableOperationActions = null;
                operationActionsToBind.forEach(this::bindOperationActionGraphicalProperties);
            });
        }
        return this;
    }

    <A, R> void bindOperationActionGraphicalProperties(OperationAction<A, R> executableOperationAction) {
        if (bindOperationActionGraphicalPropertiesNow(executableOperationAction))
            return;
        if (notYetBoundExecutableOperationActions == null)
            notYetBoundExecutableOperationActions = new ArrayList<>();
        notYetBoundExecutableOperationActions.add(executableOperationAction);
    }

    private <A, R> boolean bindOperationActionGraphicalPropertiesNow(OperationAction<A, R> executableOperationAction) {
        // The binding is possible only if a graphical action has been registered for that operation
        // Instantiating an operation request just to have the request class or operation code
        A operationRequest = newOperationActionRequest(executableOperationAction);
        // Then getting the graphical action from it
        Action graphicalAction = getGraphicalActionFromOperationRequest(operationRequest);
        // If this is not the case, we return false (can't do the binding now)
        if (graphicalAction == null)
            return false;
        // if we reach this point, we can do the binding.
        // We notify the application code that
        if (!executableOperationActionNotifyingProperties.isEmpty()) {
            Object code = getOperationCodeFromGraphicalAction(graphicalAction);
            if (code != null) {
                ObjectProperty<OperationAction> operationActionProperty = executableOperationActionNotifyingProperties.remove(code);
                // Note: 👆 should we use get() instead of remove() to keep them for a possible future use in getOrWaitOperationAction() ?
                if (operationActionProperty != null)
                    operationActionProperty.set(executableOperationAction);
            }
        }
        ActionBinder.bindWritableActionToAction(executableOperationAction, graphicalAction);
        return true;
    }

    public <A, R> Action getGraphicalActionFromOperationAction(OperationAction<A, R> operationAction) {
        // Instantiating an operation request just to have the request class or operation code
        A operationRequest = newOperationActionRequest(operationAction);
        // Then getting the graphical action from it
        return getGraphicalActionFromOperationRequest(operationRequest);
    }

    public Action getGraphicalActionFromOperationRequest(Object operationRequest) {
        // Trying to get the operation action registered with the operation request class or code.
        Action graphicalAction = getGraphicalActionFromOperationRequestClass(operationRequest.getClass());
        if (graphicalAction == null && operationRequest instanceof HasOperationCode)
            graphicalAction = getGraphicalActionFromOperationCode(((HasOperationCode) operationRequest).getOperationCode());
        return graphicalAction;
    }

    private Action getGraphicalActionFromOperationRequestClass(Class operationRequestClass) {
        return getGraphicalActionFromOperationCode(operationRequestClass); // because they share the same map
    }

    private Action getGraphicalActionFromOperationCode(Object operationCode) {
        synchronized (registeredGraphicalActions) {
            return registeredGraphicalActions.get(operationCode);
        }
    }

    private Object getOperationCodeFromGraphicalAction(Action graphicalAction) {
        synchronized (registeredGraphicalActions) {
            for (Map.Entry<Object, Action> entry : registeredGraphicalActions.entrySet()) {
                if (entry.getValue() == graphicalAction)
                    return entry.getKey();
            }
            return null;
        }
    }

    public void setOperationActionGraphicalPropertiesUpdater(Consumer<OperationAction> operationActionGraphicalPropertiesUpdater) {
        this.operationActionGraphicalPropertiesUpdater = operationActionGraphicalPropertiesUpdater;
    }

    <A, R> void updateOperationActionGraphicalProperties(OperationAction<A, R> operationAction) {
        if (operationActionGraphicalPropertiesUpdater != null)
            operationActionGraphicalPropertiesUpdater.accept(operationAction);
    }

    private ObservableValue<OperationAction> executableOperationActionNotifyingProperty(Object operationCode) {
        return executableOperationActionNotifyingProperties.computeIfAbsent(operationCode, k -> new SimpleObjectProperty<>());
    }

    public <A, R> A newOperationActionRequest(OperationAction<A, R> operationAction) {
        if (operationAction == null)
            return null;
        Function<ActionEvent, A> operationRequestFactory = operationAction.getOperationRequestFactory();
        if (operationRequestFactory != null)
            return operationRequestFactory.apply(new ActionEvent());
        return null;
    }

    public Action getOrWaitOperationAction(Object operationCode) {
        // Waiting to be notified
        ObservableValue<OperationAction> operationActionProperty = executableOperationActionNotifyingProperty(operationCode);
        // For now, we create a wrapper action that delegates the execution to the operation action (if set)
        WritableAction wrapperAction = new WritableAction(e -> { // Invisible & disabled at this stage
            OperationAction operationAction = operationActionProperty.getValue();
            if (operationAction != null) {
                operationAction.handle(e);
            }
        });
        FXProperties.onPropertySet(operationActionProperty, operationAction ->
            ActionBinder.bindWritableActionToAction(wrapperAction, operationAction)
        );
        return wrapperAction;
    }

}
