package dev.webfx.stack.authz.client.operation;

import dev.webfx.extras.operation.HasOperationCode;
import dev.webfx.stack.authz.client.spi.impl.inmemory.AuthorizationRuleType;
import dev.webfx.stack.authz.client.spi.impl.inmemory.SimpleInMemoryAuthorizationRuleBase;

/**
 * @author Bruno Salmon
 */
public final class OperationAuthorizationRule extends SimpleInMemoryAuthorizationRuleBase {

    private final Object operationRequestCode;

    public OperationAuthorizationRule(AuthorizationRuleType type, Class<?> operationRequestClass) {
        this(type, operationRequestClass, null);

    }

    public OperationAuthorizationRule(AuthorizationRuleType type, Object operationRequestCode) {
        this(type, null, operationRequestCode);
    }

    public OperationAuthorizationRule(AuthorizationRuleType type, Class<?> operationRequestClass, Object operationRequestCode) {
        super(type, operationRequestClass);
        this.operationRequestCode = operationRequestCode;
    }

    @Override
    protected boolean matchRule(Object operationRequest) {
        // Case when this rule has an operation code that matches this rule
        if (operationRequestCode != null && operationRequest instanceof HasOperationCode) {
            Object operationCode = ((HasOperationCode) operationRequest).getOperationCode();
            // Exact match
            if (operationRequestCode.equals(operationCode))
                return true;
            // Wildcard match. Note: for now superuser has 2 rules: grant operation:* and grant route:*
            // grant operation:* shouldn't include grant route:* so we need to exclude any route operation with wildcard here
            if ("*".equals(operationRequestCode) && !operationCode.toString().startsWith("RouteTo"))
                return true;
        }
        // Case when this rule has an operation request class that is the same as the passed operation request
        Class<?> operationRequestClass = operationRequestClass();
        if (operationRequestClass != null && operationRequestClass.equals(operationRequest.getClass()))
            return true;
        // Otherwise, this means that the passed operation request doesn't match this rule
        return false;
    }
}
