package dev.webfx.stack.authz.operation;

import dev.webfx.stack.ui.operation.HasOperationCode;
import dev.webfx.stack.authz.spi.impl.inmemory.AuthorizationRuleType;
import dev.webfx.stack.authz.spi.impl.inmemory.SimpleInMemoryAuthorizationRuleBase;

/**
 * @author Bruno Salmon
 */
public final class OperationAuthorizationRule extends SimpleInMemoryAuthorizationRuleBase {

    private final Class operationRequestClass;
    private final Object operationRequestCode;

    public OperationAuthorizationRule(AuthorizationRuleType type, Class operationRequestClass) {
        this(type, operationRequestClass, null);

    }

    public OperationAuthorizationRule(AuthorizationRuleType type, Object operationRequestCode) {
        this(type, null, operationRequestCode);
    }

    public OperationAuthorizationRule(AuthorizationRuleType type, Class operationRequestClass, Object operationRequestCode) {
        super(type, operationRequestClass);
        this.operationRequestClass = operationRequestClass;
        this.operationRequestCode = operationRequestCode;
    }

    @Override
    protected boolean matchRule(Object operationRequest) {
        if (operationRequestCode != null && operationRequest instanceof HasOperationCode && operationRequestCode.equals(((HasOperationCode) operationRequest).getOperationCode()))
            return true;
        return operationRequestClass != null && operationRequestClass.equals(operationRequest.getClass());
    }
}