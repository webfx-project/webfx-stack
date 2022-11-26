package dev.webfx.stack.authz.client.factory;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.uischeduler.UiScheduler;
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
                C context = contextProperty.getValue();
                if (this.context != context || FXAuthorizationsChanged.hasAuthorizationsChanged() || value == null) {
                    value = false;
                    Rq operationRequest = operationRequestFactory.apply(context);
                    authorizationFunction.apply(operationRequest)
                            .onComplete(ar -> {
                                this.context = context;
                                if (ar.succeeded())
                                    UiScheduler.runInUiThread(() -> {
                                        // Memorizing the new value to return from now for this property
                                        value = ar.result();
                                        // We call get() to mark this property as valid again, because onInvalidating()
                                        // is called only when valid transits from false to true, but not if it stays to
                                        // true, even if the dependencies change. We want it to be each time the
                                        // dependencies change, to re-evaluate the authorization function.
                                        get(); // Marks this property as valid
                                    });
                            });
                }
            }

            @Override
            protected boolean computeValue() {
                if (value == null) // May happen on first call
                    onInvalidating(); // Now value is false, but the authorization function may be pending
                return value;
            }
        };
    }
}
