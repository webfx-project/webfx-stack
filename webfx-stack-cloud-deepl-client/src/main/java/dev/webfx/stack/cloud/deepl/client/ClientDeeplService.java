package dev.webfx.stack.cloud.deepl.client;

import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.fetch.json.JsonFetch;
import dev.webfx.stack.origin.client.ClientOrigin;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
        return ClientOrigin.getHttpServerRestUrl("/rest/deepl/" + deeplCommand);
    }

}
