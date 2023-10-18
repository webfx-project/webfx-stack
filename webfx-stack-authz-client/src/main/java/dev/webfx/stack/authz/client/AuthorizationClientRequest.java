package dev.webfx.stack.authz.client;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.AsyncUtil;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class AuthorizationClientRequest<Rq, Rs> {

    private Rq operationRequest;
    private AsyncFunction<Rq, Rs> authorizedOperationAsyncExecutor;
    private AsyncFunction<Throwable, ?> unauthorizedOperationAsyncExecutor = o -> Future.failedFuture(new UnauthorizedOperationException());

    public Rq getOperationRequest() {
        return operationRequest;
    }

    public AuthorizationClientRequest<Rq, Rs> setOperationRequest(Rq operationRequest) {
        this.operationRequest = operationRequest;
        return this;
    }

    public AsyncFunction<Rq, Rs> getAuthorizedOperationAsyncExecutor() {
        return authorizedOperationAsyncExecutor;
    }

    public AuthorizationClientRequest<Rq, Rs> onAuthorizedExecuteAsync(AsyncFunction<Rq, Rs> authorizedExecutor) {
        this.authorizedOperationAsyncExecutor = authorizedExecutor;
        return this;
    }

    public AuthorizationClientRequest<Rq, Rs> onAuthorizedExecute(Function<Rq, Rs> authorizedExecutor) {
        return onAuthorizedExecuteAsync((Rq rq) -> Future.succeededFuture(authorizedExecutor.apply(rq)));
    }

    public AuthorizationClientRequest<Rq, Rs> onAuthorizedExecute(Consumer<Rq> authorizedExecutor) {
        return onAuthorizedExecuteAsync(rq -> AsyncUtil.consumeAsync(authorizedExecutor, rq));
    }

    public AuthorizationClientRequest<Rq, Rs> onAuthorizedExecute(Runnable authorizedExecutor) {
        return onAuthorizedExecuteAsync(rq -> AsyncUtil.runAsync(authorizedExecutor));
    }

    public AsyncFunction<Throwable, ?> getUnauthorizedOperationAsyncExecutor() {
        return unauthorizedOperationAsyncExecutor;
    }

    public AuthorizationClientRequest<Rq, Rs> onUnauthorizedExecuteAsync(AsyncFunction<Throwable, ?> unauthorizedAsyncExecutor) {
        this.unauthorizedOperationAsyncExecutor = unauthorizedAsyncExecutor;
        return this;
    }

    public AuthorizationClientRequest<Rq, Rs> onUnauthorizedExecute(Consumer<Throwable> authorizedExecutor) {
        return onUnauthorizedExecuteAsync(o -> AsyncUtil.consumeAsync(authorizedExecutor, o));
    }

    public AuthorizationClientRequest<Rq, Rs> onUnauthorizedExecute(Runnable authorizedExecutor) {
        return onUnauthorizedExecuteAsync(o -> AsyncUtil.runAsync(authorizedExecutor));
    }

    public Future<Boolean> isAuthorizedAsync() {
        return AuthorizationClientService.isAuthorized(getOperationRequest());
    }

    public Future<Rs> executeAsync() {
        Promise<Rs> promise = Promise.promise();
        // Checking if the operation is authorized
        isAuthorizedAsync().onComplete(ar -> {
            if (ar.succeeded() && ar.result()) { // Yes it is authorized :)
                // We ask the authorized operation executor
                getAuthorizedOperationAsyncExecutor()
                        // to execute the operation request (also an asynchronous call)
                        .apply(getOperationRequest())
                        // and complete the promise with the result of the operation execution
                        .onComplete(promise);
            } else { // No it's not authorized (or there was an exception during the authorization check)
                // We ask the unauthorised operation executor
                getUnauthorizedOperationAsyncExecutor()
                        // to execute what's needed to report this non-authorization (ex: display a message to user)
                        .apply(ar.cause()) // Note: in general ar.cause() is null, meaning that there was no error during authorization check
                        // and we fail the promise
                        .onComplete(ignored -> {
                            if (ar.cause() != null) // Rare cases where an exception arose during authorization check
                                promise.fail(ar.cause());
                            else // General case where the authorization check was ok but result was not authorized
                                promise.fail(new UnauthorizedOperationException());
                        });
            }
        });
        return promise.future();
    }

}
