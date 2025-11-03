package dev.webfx.stack.authz.client.binder;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.stack.authz.client.context.AuthorizationContext;
import dev.webfx.stack.session.state.client.fx.FXAuthorizationsChanged;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class AuthorizationBinder {

    public static <I, Rq> ObservableBooleanValue authorizedOperationProperty(
        ObservableValue<I> inputProperty,
        Function<I, Rq> operationRequestFactory,
        AsyncFunction<Rq, Boolean> authorizationFunction) {
        return new BooleanBinding() {
            private I lastInput;
            private final Map<String, Object> lastContextProperties = new HashMap<>();
            private Boolean lastComputedValue;

            {
                // Indicating the dependencies for this property
                bind(inputProperty, FXAuthorizationsChanged.authorizationsChangedProperty(), AuthorizationContext.getContextProperties());
            }

            @Override
            protected void onInvalidating() {
                // The input property is, for example, an operationActionProperty (null first, then non-null once the
                // operations have been loaded). We get the input.
                I input = inputProperty.getValue();
                boolean authorizationCallNeeded = this.lastInput != input || FXAuthorizationsChanged.hasAuthorizationsChanged() || lastComputedValue == null || !Objects.equals(AuthorizationContext.getContextProperties(), lastContextProperties);
                if (!authorizationCallNeeded) { // No need to call the authorization this time, but:
                    // We mark this property as valid again right now, because otherwise the JavaFX API calls onInvalidating()
                    // only when valid transits from false to true, but NOT IF VALID STAYS TO TRUE, and this, even if
                    // the dependencies change. But we want this method to be called each time the dependencies change,
                    // because we need to eventually call the asynchronous authorization function when this happens.
                    markAsValid();
                } else {
                    // Because we don't know the result of the authorization function yet, we set the value to false
                    // by default (better to not authorize the user for now until we really know the authorization result).
                    lastComputedValue = false;
                    // We generate the request from the input and pass it to the authorization function and wait its completion
                    Rq operationRequest = operationRequestFactory.apply(input);
                    authorizationFunction.apply(operationRequest)
                        .onComplete(ar -> {
                            this.lastInput = input;
                            lastContextProperties.clear();
                            lastContextProperties.putAll(AuthorizationContext.getContextProperties());
                            // Memorizing the new value to return from now for this property
                            if (ar.succeeded())
                                lastComputedValue = ar.result();
                            // We call markAsValid() for the same reason explained above
                            markAsValid();
                        });
                }
            }

            private void markAsValid() {
                get(); // Calling get() marks the property as valid again
            }

            @Override
            protected boolean computeValue() {
                if (lastComputedValue == null) // This happens on the first call
                    onInvalidating(); // Now the value is false, but the authorization function is pending and may change the value later
                return lastComputedValue;
            }
        };
    }
}
