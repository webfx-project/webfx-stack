package dev.webfx.stack.i18n.operations;

import dev.webfx.stack.i18n.I18n;
import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
final class ChangeLanguageExecutor {

    static Future<Void> executeRequest(ChangeLanguageRequest rq) {
        return execute(rq.getLanguage());
    }

    private static Future<Void> execute(Object language) {
        I18n.setLanguage(language);
        return Future.succeededFuture();
    }
}
