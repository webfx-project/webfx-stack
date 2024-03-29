package dev.webfx.stack.authz.client.factory;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.function.Factory;
import dev.webfx.stack.authz.client.AuthorizationClientRequest;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class AuthorizationFactory {

    public static <Rq, Rs> AuthorizationClientRequest<Rq, Rs> newAuthorizationRequest() {
        return new AuthorizationClientRequest<>();
    }

    public static <Rq, Rs> AuthorizationClientRequest<Rq, Rs> newAuthorizationRequest(Rq operationRequest) {
        return AuthorizationFactory.<Rq, Rs>newAuthorizationRequest().setOperationRequest(operationRequest);
    }

    public static Future<Boolean> isAuthorized(Object operationRequest) {
        return newAuthorizationRequest(operationRequest).isAuthorizedAsync();
    }

    public static ObservableBooleanValue authorizedOperationProperty(Object operationRequest) {
        return authorizedOperationProperty(() -> operationRequest);
    }

    public static ObservableBooleanValue authorizedOperationProperty(Factory operationRequestFactory) {
        return authorizedOperationProperty(new SimpleObjectProperty<>(), ignored -> operationRequestFactory.create());
    }

    public static <C> ObservableBooleanValue authorizedOperationProperty(ObservableValue<C> observableContext, Function<C, ?> operationRequestFactory ) {
        return AuthorizationUtil.authorizedOperationProperty(operationRequestFactory, AuthorizationFactory::isAuthorized, observableContext);
    }

}
