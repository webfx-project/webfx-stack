package dev.webfx.stack.framework.client.operations.i18n;

import dev.webfx.stack.framework.shared.operation.HasOperationExecutor;
import dev.webfx.stack.platform.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public class ChangeLanguageRequest implements HasOperationExecutor<ChangeLanguageRequest, Void> {

    private final Object language;

    public ChangeLanguageRequest(Object language) {
        this.language = language;
    }

    public Object getLanguage() {
        return language;
    }

    @Override
    public AsyncFunction<ChangeLanguageRequest, Void> getOperationExecutor() {
        return ChangeLanguageExecutor::executeRequest;
    }
}
