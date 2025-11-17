package dev.webfx.stack.authz.client.spi.impl.inmemory;

/**
 * @author Bruno Salmon
 */
public abstract class SimpleInMemoryAuthorizationRuleBase implements InMemoryAuthorizationRule {

    private final AuthorizationRuleType type;
    private final Class<?> operationRequestClass;

    public SimpleInMemoryAuthorizationRuleBase(AuthorizationRuleType type, Class<?> operationRequestClass) {
        this.type = type;
        this.operationRequestClass = operationRequestClass;
    }

    @Override
    public AuthorizationRuleResult computeRuleResult(Object authorizationRequest) {
        boolean matchRule = matchRule(authorizationRequest);
        if (!matchRule)
            return AuthorizationRuleResult.OUT_OF_RULE_CONTEXT;
        return type == AuthorizationRuleType.GRANT ? AuthorizationRuleResult.GRANTED : AuthorizationRuleResult.DENIED;
    }

    protected abstract boolean matchRule(Object operationRequest);

    @Override
    public Class<?> operationRequestClass() {
        return operationRequestClass;
    }
}
