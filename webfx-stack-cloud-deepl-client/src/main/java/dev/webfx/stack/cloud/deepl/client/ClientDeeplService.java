package dev.webfx.stack.cloud.deepl.client;

import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.fetch.json.JsonFetch;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class ClientDeeplService {

    public static Future<String> translate(String text, String sourceLanguage, String targetLanguage) {
        return JsonFetch.fetchJsonObject(getDeeplRestApiUrl("translate")
                                         + "?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                                         + "&source_lang=" + sourceLanguage.toUpperCase()
                                         + "&target_lang=" + targetLanguage.toUpperCase())
            .compose(astObject -> {
                ReadOnlyAstArray translated = astObject.getArray("translations");
                if (translated != null && !translated.isEmpty())
                    return Future.succeededFuture(translated.getObject(0).get("text"));
                return Future.failedFuture("No translation received from service. Please check DEEPL_API_KEY variable is correctly set");
            });
    }

    public static Future<Integer> usage() {
        return JsonFetch.fetchJsonObject(getDeeplRestApiUrl("usage" ))
            .compose(astObject -> {
                int characterLimit = astObject.getInteger("character_limit");
                int characterCount = astObject.getInteger("character_count");
                return Future.succeededFuture(characterLimit - characterCount);
            });
    }

    private static String getDeeplRestApiUrl(String deeplCommand) {
        return getHttpServerOrigin() + "/rest/deepl/" + deeplCommand;
    }

    private static String getHttpServerOrigin() {
        String origin = evaluateOrNull("${{ HTTP_SERVER_ORIGIN }}");
        if (origin == null)
            origin = "https://" + evaluateOrNull("${{ HTTP_SERVER_HOST | BUS_SERVER_HOST | SERVER_HOST }}");
        return origin;
    }

    private static String evaluateOrNull(String expression) {
        String value = ConfigLoader.getRootConfig().get(expression);
        if (Objects.equals(value, expression))
            value = null;
        return value;
    }

}
