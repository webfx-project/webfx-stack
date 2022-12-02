package dev.webfx.stack.authz.client.factory;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.stack.session.state.client.fx.FXAuthorizationsChanged;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class AuthorizationUtil {

    public static <C, Rq> ObservableBooleanValue authorizedOperationProperty(Function<C, Rq> operationRequestFactory, AsyncFunction<Rq, Boolean> authorizationFunction, ObservableValue<C> contextProperty) {
        return new BooleanBinding() {
            C context;
            Boolean value;

            {
                // Indicating the dependencies for this property
                bind(contextProperty, FXAuthorizationsChanged.authorizationsChangedProperty());
            }

            @Override
            protected void onInvalidating() {
                // The context property is for example an operationActionProperty (null first, then non-null once the
                // operations have been loaded). We get the context.
                C context = contextProperty.getValue();
                boolean authorizationCallNeeded = this.context != context || FXAuthorizationsChanged.hasAuthorizationsChanged() || value == null;
                if (!authorizationCallNeeded) { // No need to call the authorization this time, but:
                    // We mark this property as valid again right now, because otherwise the JavaFX API calls onInvalidating()
                    // only when valid transits from false to true, but NOT IF VALID STAYS TO TRUE, and this, even if
                    // the dependencies change. But we want this method to be called each time the dependencies change,
                    // because we need to eventually call the asynchronous authorization function when this happens.
                    markAsValid();
                } else {
                    // Because we don't know yet the result of the authorization function, we set the value to false
                    // by default (better to not authorize the user for now until we really know the authorization result).
                    value = false;
                    // We generate the request from the context, and pass it to the authorization function and wait its completion
                    Rq operationRequest = operationRequestFactory.apply(context);
                    authorizationFunction.apply(operationRequest)
                            .onComplete(ar -> {
                                this.context = context;
                                // Memorizing the new value to return from now for this property
                                if (ar.succeeded())
                                    value = ar.result();
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
                if (value == null) // This happens on first call
                    onInvalidating(); // Now value is false, but the authorization function is pending and may change the value later
                return value;
            }
        };
    }
}
