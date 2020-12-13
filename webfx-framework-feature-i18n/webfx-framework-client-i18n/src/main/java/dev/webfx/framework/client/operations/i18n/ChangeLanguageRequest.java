package dev.webfx.framework.client.operations.i18n;

import dev.webfx.framework.shared.operation.HasOperationExecutor;
import dev.webfx.platform.shared.util.async.AsyncFunction;

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
