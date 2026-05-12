package dev.webfx.stack.cloud.deepl.server;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.fetch.FetchOptions;
import dev.webfx.platform.fetch.Headers;
import dev.webfx.platform.util.vertx.VertxInstance;

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
            .handler(ctx -> {
                // Read the raw request body without activating Netty's form decoder.
                // BodyHandler.create() would call setExpectMultipart(true) which enforces
                // Vert.x 5's default maxFormAttributeSize of 8 KB — too small for HTML content.
                // The client already sends a properly URL-encoded body via URLSearchParams,
                // so we forward it verbatim to DeepL without any re-encoding.
                ctx.request().body()
                    .onFailure(error -> ctx.response().setStatusCode(500).end(error.getMessage()))
                    .onSuccess(body -> {
                        String url = deeplApiUrl + deeplCommand;
                        String rawBody = body.toString();
                        FetchOptions options = new FetchOptions()
                                .setMethod("POST")
                                .setHeaders(Headers.create()
                                    .append("Authorization", "DeepL-Auth-Key " + deeplApiKey)
                                    .append("Content-Type", "application/x-www-form-urlencoded")
                                );
                        if (!rawBody.isEmpty()) {
                            options.setBody(rawBody);
                        }
                        Fetch.fetch(url, options)
                            .onFailure(error -> ctx.response().setStatusCode(500).end(error.getMessage()))
                            .onSuccess(response -> {
                                response.text()
                                    .onSuccess(text -> ctx.response().end(text))
                                    .onFailure(error -> ctx.response().setStatusCode(500).end(error.getMessage()));
                            });
                    });
            });
    }
}
