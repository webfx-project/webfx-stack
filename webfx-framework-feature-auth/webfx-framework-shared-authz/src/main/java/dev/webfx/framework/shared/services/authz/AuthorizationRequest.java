package dev.webfx.framework.shared.services.authz;

import dev.webfx.platform.shared.util.async.AsyncFunction;
import dev.webfx.platform.shared.util.async.AsyncUtil;
import dev.webfx.platform.shared.util.async.Future;
import dev.webfx.platform.shared.util.async.Promise;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class AuthorizationRequest<Rq, Rs> {

    private Object userPrincipal;
    private Rq operationRequest;
    private AsyncFunction<Rq, Rs> authorizedOperationAsyncExecutor;
    private AsyncFunction<Throwable, ?> unauthorizedOperationAsyncExecutor = o -> Future.failedFuture(new UnauthorizedOperationException());

    public Object getUserPrincipal() {
        return userPrincipal;
    }

    public AuthorizationRequest<Rq, Rs> setUserPrincipal(Object userPrincipal) {
        this.userPrincipal = userPrincipal;
        return this;
    }

    public Rq getOperationRequest() {
        return operationRequest;
    }

    public AuthorizationRequest<Rq, Rs> setOperationRequest(Rq operationRequest) {
        this.operationRequest = operationRequest;
        return this;
    }

    public AsyncFunction<Rq, Rs> getAuthorizedOperationAsyncExecutor() {
        return authorizedOperationAsyncExecutor;
    }

    public AuthorizationRequest<Rq, Rs> onAuthorizedExecuteAsync(AsyncFunction<Rq, Rs> authorizedExecutor) {
        this.authorizedOperationAsyncExecutor = authorizedExecutor;
        return this;
    }

    public AuthorizationRequest<Rq, Rs> onAuthorizedExecute(Function<Rq, Rs> authorizedExecutor) {
        return onAuthorizedExecuteAsync((Rq rq) -> Future.succeededFuture(authorizedExecutor.apply(rq)));
    }

    public AuthorizationRequest<Rq, Rs> onAuthorizedExecute(Consumer<Rq> authorizedExecutor) {
        return onAuthorizedExecuteAsync(rq -> AsyncUtil.consumeAsync(authorizedExecutor, rq));
    }

    public AuthorizationRequest<Rq, Rs> onAuthorizedExecute(Runnable authorizedExecutor) {
        return onAuthorizedExecuteAsync(rq -> AsyncUtil.runAsync(authorizedExecutor));
    }

    public AsyncFunction<Throwable, ?> getUnauthorizedOperationAsyncExecutor() {
        return unauthorizedOperationAsyncExecutor;
    }

    public AuthorizationRequest<Rq, Rs> onUnauthorizedExecuteAsync(AsyncFunction<Throwable, ?> unauthorizedAsyncExecutor) {
        this.unauthorizedOperationAsyncExecutor = unauthorizedAsyncExecutor;
        return this;
    }

    public AuthorizationRequest<Rq, Rs> onUnauthorizedExecute(Consumer<Throwable> authorizedExecutor) {
        return onUnauthorizedExecuteAsync(o -> AsyncUtil.consumeAsync(authorizedExecutor, o));
    }

    public AuthorizationRequest<Rq, Rs> onUnauthorizedExecute(Runnable authorizedExecutor) {
        return onUnauthorizedExecuteAsync(o -> AsyncUtil.runAsync(authorizedExecutor));
    }

    public Future<Boolean> isAuthorizedAsync() {
        return AuthorizationService.isAuthorized(getOperationRequest(), getUserPrincipal());
    }

    public Future<Rs> executeAsync() {
        Promise<Rs> promise = Promise.promise();
        isAuthorizedAsync().onComplete(ar -> {
            if (ar.succeeded() && ar.result())
                getAuthorizedOperationAsyncExecutor().apply(getOperationRequest()).onComplete(ar2 -> promise.complete(ar2.result()));
            else
                getUnauthorizedOperationAsyncExecutor().apply(ar.cause()).onComplete(ar2 -> promise.fail(ar.cause()));
        });
        return promise.future();
    }

}
