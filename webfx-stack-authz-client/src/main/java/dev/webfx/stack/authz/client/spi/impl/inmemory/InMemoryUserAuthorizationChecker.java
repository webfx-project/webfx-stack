package dev.webfx.stack.authz.client.spi.impl.inmemory;

import dev.webfx.stack.authz.client.spi.impl.UserAuthorizationChecker;
import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.util.FutureBroadcaster;
import dev.webfx.platform.async.Handler;

/**
 * @author Bruno Salmon
 */
public class InMemoryUserAuthorizationChecker implements UserAuthorizationChecker {

    protected final InMemoryAuthorizationRuleRegistry ruleRegistry;
    private FutureBroadcaster<?> rulesLoadingBroadcaster;

    public InMemoryUserAuthorizationChecker() {
        this(new InMemoryAuthorizationRuleRegistry());
    }

    public InMemoryUserAuthorizationChecker(InMemoryAuthorizationRuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public Future<Boolean> isAuthorized(Object operationAuthorizationRequest) {
        FutureBroadcaster<?> loader = rulesLoadingBroadcaster;
        return loader == null ? // means the rules are already loaded, so we can evaluate them and return the result immediately
                Future.succeededFuture(ruleRegistry.doesRulesAuthorize(operationAuthorizationRequest))
                // Otherwise, we first need to wait the rules to be loaded and only then we can evaluate them and return the result
                : loader.newClient().compose(result -> Future.succeededFuture(ruleRegistry.doesRulesAuthorize(operationAuthorizationRequest)));
    }

    protected <T> void setUpInMemoryAsyncRulesLoading(Future<T> loadingFuture, Handler<AsyncResult<T>> loadedHandler) {
        FutureBroadcaster<T> broadcaster = new FutureBroadcaster<>(loadingFuture);
        rulesLoadingBroadcaster = broadcaster;
        broadcaster.newClient().onComplete(loadedHandler);
        broadcaster.newClient().onComplete(ar -> rulesLoadingBroadcaster = null);
    }
}
