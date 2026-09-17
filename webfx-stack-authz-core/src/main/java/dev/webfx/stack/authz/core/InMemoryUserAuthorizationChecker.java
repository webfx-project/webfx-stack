package dev.webfx.stack.authz.core;

import dev.webfx.platform.async.Future;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Answers authorization questions from rules held in memory.
 *
 * @author Bruno Salmon
 */
public class InMemoryUserAuthorizationChecker implements UserAuthorizationChecker {

    protected final InMemoryAuthorizationRuleRegistry ruleRegistry;
    private final Supplier<Map<String, String>> contextProvider;

    public InMemoryUserAuthorizationChecker() {
        this(new InMemoryAuthorizationRuleRegistry());
    }

    public InMemoryUserAuthorizationChecker(InMemoryAuthorizationRuleRegistry ruleRegistry) {
        this(ruleRegistry, Collections::emptyMap);
    }

    /**
     * @param contextProvider where to read the caller's context when a question is asked. A UI passes
     *   its ambient context (what the user is looking at); a server passes something that resolves the
     *   context of the request being handled. Called once per question rather than captured, so a
     *   caller whose context changes — either kind — is answered against the current one.
     */
    public InMemoryUserAuthorizationChecker(InMemoryAuthorizationRuleRegistry ruleRegistry,
                                            Supplier<Map<String, String>> contextProvider) {
        this.ruleRegistry = ruleRegistry;
        this.contextProvider = contextProvider == null ? Collections::emptyMap : contextProvider;
    }

    @Override
    public Future<Boolean> isAuthorized(Object operationRequest) {
        Map<String, String> context = contextProvider.get();
        return Future.succeededFuture(ruleRegistry.doesRulesAuthorize(operationRequest,
            context == null ? Collections.emptyMap() : context));
    }

}
