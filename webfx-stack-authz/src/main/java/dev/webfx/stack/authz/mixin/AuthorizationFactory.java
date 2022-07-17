package dev.webfx.stack.authz.mixin;

import dev.webfx.stack.authz.AuthorizationRequest;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import dev.webfx.stack.async.Future;
import dev.webfx.platform.util.function.Factory;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public interface AuthorizationFactory extends HasUserPrincipal {

    default <Rq, Rs> AuthorizationRequest<Rq, Rs> newAuthorizationRequest() {
        return new AuthorizationRequest<Rq, Rs>().setUserPrincipal(getUserPrincipal());
    }

    default <Rq, Rs> AuthorizationRequest<Rq, Rs> newAuthorizationRequest(Rq operationRequest) {
        return this.<Rq, Rs>newAuthorizationRequest().setOperationRequest(operationRequest);
    }

    default Future<Boolean> isAuthorized(Object operationRequest) {
        return newAuthorizationRequest(operationRequest).isAuthorizedAsync();
    }

    default ObservableBooleanValue authorizedOperationProperty(Object operationRequest) {
        return authorizedOperationProperty(() -> operationRequest);
    }

    default ObservableBooleanValue authorizedOperationProperty(Factory operationRequestFactory) {
        return authorizedOperationProperty(new SimpleObjectProperty<>(), ignored -> operationRequestFactory.create());
    }

    default <C> ObservableBooleanValue authorizedOperationProperty(ObservableValue<C> observableContext, Function<C, ?> operationRequestFactory ) {
        return AuthorizationUtil.authorizedOperationProperty(operationRequestFactory, this::isAuthorized, observableContext, this instanceof HasUserPrincipalProperty ? ((HasUserPrincipalProperty) this).userPrincipalProperty() : null);
    }

}