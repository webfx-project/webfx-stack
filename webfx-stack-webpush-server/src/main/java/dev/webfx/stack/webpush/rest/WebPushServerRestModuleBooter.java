package dev.webfx.stack.webpush.rest;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.platform.util.vertx.VertxInstance;
import dev.webfx.stack.routing.router.spi.impl.vertx.VertxRoutingContext;
import dev.webfx.stack.webpush.WebPushConfig;
import dev.webfx.stack.webpush.WebPushServerService;
import dev.webfx.stack.webpush.handler.RotateSubscriptionHandler;
import dev.webfx.stack.webpush.impl.WebPushServiceImpl;
import dev.webfx.stack.webpush.spi.WebPushServerServiceProvider;
import dev.webfx.stack.webpush.spi.WebPushSubscriptionStore;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;

import java.util.ServiceLoader;

/**
 * Module boot for {@code webfx-stack-webpush-server}.
 *
 * <h2>What it does at startup</h2>
 * <ol>
 *   <li>Waits for the application config to finish loading, then reads the
 *       {@code webpush.vapid} subtree (with {@code ${{ VAR }}} placeholders
 *       fully resolved against the deployment's variable files). If the
 *       VAPID keys are missing (dev / opt-out builds), skips the rest of
 *       the wiring with a single log line — the module becomes a no-op
 *       rather than a startup failure.</li>
 *   <li>Constructs the default {@link WebPushServerServiceProvider} and
 *       registers it so the {@link WebPushServerService} static facade
 *       works application-wide.</li>
 *   <li>If a {@link WebPushSubscriptionStore} impl is registered (the host
 *       application provides one — KBS wires it via modality entities),
 *       registers the rotate-subscription REST route. Without a store impl
 *       the route can't function, so we skip it.</li>
 * </ol>
 *
 * @author Bruno Salmon
 */
public final class WebPushServerRestModuleBooter implements ApplicationModuleBooter {

    private static final String MODULE_NAME = "webfx-stack-webpush-server";

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    public int getBootLevel() {
        return COMMUNICATION_REGISTER_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        // Defer to onConfigLoaded so the ${{ VAR }} placeholders in the
        // webpush.vapid subtree are resolved against the deployment's
        // variable files (loaded from /opt/kbs/conf/ by the file-based
        // config plugin). Reading SourcesConfig directly here would only
        // see the raw classpath bundle — placeholders would leak through.
        ConfigLoader.onConfigLoaded(WebPushConfig.CONFIG_PATH, this::onWebPushConfigLoaded);
    }

    private void onWebPushConfigLoaded(Config config) {
        WebPushConfig webPushConfig = WebPushConfig.from(config);
        if (webPushConfig == null) {
            Console.log("[" + MODULE_NAME + "] VAPID config absent — Web Push disabled");
            return;
        }

        // Register the provider so application code can reach it via the
        // WebPushServerService static facade.
        SingleServiceProvider.registerServiceProvider(
                WebPushServerServiceProvider.class, new WebPushServiceImpl(webPushConfig));

        // Wire the rotate-subscription REST endpoint — only if the host
        // application registered a store implementation, since the route
        // can't function without one. The ServiceLoader lookup is the
        // standard webfx pattern; the application's impl module declares
        // `provides WebPushSubscriptionStore with ...` in its module-info.
        WebPushSubscriptionStore store = ServiceLoader.load(WebPushSubscriptionStore.class)
                .findFirst()
                .orElse(null);
        if (store == null) {
            Console.log("[" + MODULE_NAME + "] No WebPushSubscriptionStore registered — rotate endpoint not wired");
            return;
        }

        // VertxInstance.getHttpRouter() returns the Vert.x router directly
        // (not the webfx-stack abstraction) — same pattern as modality-base-rest-map.
        // We wrap each invocation in a VertxRoutingContext so the handler still
        // talks to the webfx abstraction (forward-compatible if the routing
        // surface ever migrates away from the Vert.x escape hatches).
        Router router = VertxInstance.getHttpRouter();
        RotateSubscriptionHandler handler = new RotateSubscriptionHandler(store);
        router.route(HttpMethod.POST, "/rest/push/rotate-subscription")
                .handler(vertxCtx -> handler.handle(VertxRoutingContext.create(vertxCtx)));

        Console.log("[" + MODULE_NAME + "] Web Push initialised (rotate endpoint registered)");
    }
}
