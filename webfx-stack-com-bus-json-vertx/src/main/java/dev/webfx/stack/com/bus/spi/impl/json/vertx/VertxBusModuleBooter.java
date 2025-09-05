package dev.webfx.stack.com.bus.spi.impl.json.vertx;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.util.vertx.VertxInstance;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;


/**
 * @author Bruno Salmon
 */
public class VertxBusModuleBooter implements ApplicationModuleBooter {

    private final static String CONFIG_PATH = "webfx.stack.com.bus.vertx";
    private final static String BUS_PREFIX_CONFIG_KEY = "busPrefix";
    private final static String PING_TIMEOUT_KEY = "pingTimeout";

    @Override
    public String getModuleName() {
        return "webfx-stack-com-bus-json-vertx";
    }

    @Override
    public int getBootLevel() {
        return CONF_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        // 1) Configuring the database connection
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> {

            if (config == null) {
                log("❌ No Vert.x bus configuration was not found at " + CONFIG_PATH);
                return;
            }

            String busPrefix = config.getString(BUS_PREFIX_CONFIG_KEY);
            Long pingTimeout = config.getLong(PING_TIMEOUT_KEY);
            if (busPrefix == null || pingTimeout == null) {
                log("❌ Invalid Vert.x bus configuration at " + CONFIG_PATH);
                return;
            }

            VertxInstance.setBridgeInstaller(() -> {
                VertxInstance.getHttpRouter()
                    .route("/" + busPrefix + "/*")
                    .handler(BodyHandler.create()) // Required since Vert.x 4.3.0 to handle XHR POST requests (see https://github.com/vert-x3/vertx-web/issues/2188)
                    .subRouter(SockJSHandler.create(VertxInstance.getVertx())
                        .bridge(new SockJSBridgeOptions()
                                .setPingTimeout(pingTimeout) // Should be higher than client WebSocketBusOptions.pingInterval (which is set to 30_000 at the time of writing this code)
                                .addInboundPermitted(new PermittedOptions(new JsonObject()))
                                .addOutboundPermitted(new PermittedOptions(new JsonObject()))
                            , bridgeEvent -> { // Calling the VertxInstance bridge event handler if set
                                Handler<BridgeEvent> bridgeEventHandler = VertxInstance.getBridgeEventHandler();
                                if (bridgeEventHandler != null)
                                    bridgeEventHandler.handle(bridgeEvent);
                                else
                                    bridgeEvent.complete(true);
                            }
                        )
                    );
                log("✓ Vert.x bus configured with prefix = '" + busPrefix + "' & timeout = " + pingTimeout + " ms");
            });

        });
    }
}
