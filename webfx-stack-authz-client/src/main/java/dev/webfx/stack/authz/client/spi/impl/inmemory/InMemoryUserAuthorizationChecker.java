package dev.webfx.stack.authz.client.spi.impl.inmemory;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authz.client.spi.impl.UserAuthorizationChecker;

/**
 * @author Bruno Salmon
 */
public class InMemoryUserAuthorizationChecker implements UserAuthorizationChecker {

    protected final InMemoryAuthorizationRuleRegistry ruleRegistry;

    public InMemoryUserAuthorizationChecker() {
        this(new InMemoryAuthorizationRuleRegistry());
    }

    public InMemoryUserAuthorizationChecker(InMemoryAuthorizationRuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public Future<Boolean> isAuthorized(Object operationRequest) {
        return Future.succeededFuture(ruleRegistry.doesRulesAuthorize(operationRequest));
    }

}
