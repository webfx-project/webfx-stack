package dev.webfx.stack.cloud.deepl.server;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.fetch.FetchOptions;
import dev.webfx.platform.fetch.Headers;
import dev.webfx.platform.util.vertx.VertxInstance;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.handler.BodyHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author Bruno Salmon
 */
public class ServerDeeplModuleBooter implements ApplicationModuleBooter {

    private static final String CONFIG_PATH = "webfx.stack.cloud.deepl.server";

    private String deeplApiKey;
    private String deeplApiUrl;

    @Override
    public String getModuleName() {
        return "webfx-stack-cloud-deepl-server";
    }

    @Override
    public int getBootLevel() {
        return ApplicationModuleBooter.COMMUNICATION_REGISTER_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> {
            deeplApiKey = config.getString("deeplApiKey");
            deeplApiUrl = config.getString("deeplApiUrl");
            if (deeplApiUrl == null && deeplApiKey != null) {
                deeplApiUrl = "https://api" + (deeplApiKey.endsWith(":fx") ? "-free" : "") + ".deepl.com/v2/";
            }
            proxyDeeplApi("translate");
            proxyDeeplApi("usage");
        });
    }

    private void proxyDeeplApi(String deeplCommand) {
        VertxInstance.getHttpRouter().route("/rest/deepl/" + deeplCommand + "*")
            .handler(BodyHandler.create())
            .handler(ctx -> {
                HttpServerRequest request = ctx.request();
                String url = deeplApiUrl + deeplCommand + (request.params().isEmpty() ? "" : "?" + extractQueryParameters(request));
                Fetch.fetch(url, new FetchOptions()
                        .setHeaders(Headers.create()
                            .append("Authorization", "DeepL-Auth-Key " + deeplApiKey)
                        )
                    )
                    .onFailure(error -> ctx.response().setStatusCode(500).end(error.getMessage()))
                    .onSuccess(response -> {
                        response.text()
                            .onSuccess(text -> ctx.response().end(text))
                            .onFailure(error -> ctx.response().setStatusCode(500).end(error.getMessage()));
                    });
            });
    }

    private String extractQueryParameters(HttpServerRequest request) {
        StringBuilder params = new StringBuilder();
        request.params().forEach(entry -> {
            if (!params.isEmpty()) {
                params.append("&");
            }
            params.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        });
        return params.toString();
    }
}
