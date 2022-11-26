package dev.webfx.stack.authz.client.operation;

import dev.webfx.stack.authz.client.spi.impl.inmemory.AuthorizationRuleType;
import dev.webfx.stack.authz.client.spi.impl.inmemory.SimpleInMemoryAuthorizationRuleBase;
import dev.webfx.stack.ui.operation.HasOperationCode;

/**
 * @author Bruno Salmon
 */
public final class OperationAuthorizationRule<R> extends SimpleInMemoryAuthorizationRuleBase<R> {

    private final Object operationRequestCode;

    public OperationAuthorizationRule(AuthorizationRuleType type, Class<R> operationRequestClass) {
        this(type, operationRequestClass, null);

    }

    public OperationAuthorizationRule(AuthorizationRuleType type, Object operationRequestCode) {
        this(type, null, operationRequestCode);
    }

    public OperationAuthorizationRule(AuthorizationRuleType type, Class operationRequestClass, Object operationRequestCode) {
        super(type, operationRequestClass);
        this.operationRequestCode = operationRequestCode;
    }

    @Override
    protected boolean matchRule(Object operationRequest) {
        if (operationRequestCode != null && operationRequest instanceof HasOperationCode && operationRequestCode.equals(((HasOperationCode) operationRequest).getOperationCode()))
            return true;
        Class operationRequestClass = operationRequestClass();
        return operationRequestClass != null && operationRequestClass.equals(operationRequest.getClass());
    }
}
