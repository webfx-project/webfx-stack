package dev.webfx.stack.authn.logout.client.operation;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.stack.i18n.HasI18nKey;
import dev.webfx.stack.ui.operation.HasOperationCode;
import dev.webfx.stack.ui.operation.HasOperationExecutor;

/**
 * @author Bruno Salmon
 */
public final class LogoutRequest implements HasOperationCode, HasI18nKey, HasOperationExecutor<LogoutRequest, Void> {

    private final static String OPERATION_CODE = "Logout";


    @Override
    public Object getOperationCode() {
        return OPERATION_CODE;
    }

    @Override
    public Object getI18nKey() {
        return LogoutI18nKeys.Logout;
    }

    @Override
    public AsyncFunction<LogoutRequest, Void> getOperationExecutor() {
        return LogoutExecutor::executeRequest;
    }
}
