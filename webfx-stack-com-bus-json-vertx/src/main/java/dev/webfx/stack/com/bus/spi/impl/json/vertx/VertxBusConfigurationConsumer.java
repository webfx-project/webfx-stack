package dev.webfx.stack.com.bus.spi.impl.json.vertx;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.platform.vertx.common.VertxInstance;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.impl.resource.DefaultResourceConfigurationConsumer;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

/**
 * @author Bruno Salmon
 */
public final class VertxBusConfigurationConsumer extends DefaultResourceConfigurationConsumer {

    private static final String CONFIGURATION_NAME = "VertxBusOptions";
    private final static String DEFAULT_CONFIGURATION_RESOURCE_FILE_NAME = "VertxBusOptions.default.json";
    final static String BUS_PREFIX_CONFIG_KEY = "busPrefix";
    final static String PING_TIMEOUT_KEY = "pingTimeout";

    public VertxBusConfigurationConsumer() {
        super(CONFIGURATION_NAME, DEFAULT_CONFIGURATION_RESOURCE_FILE_NAME);
    }

    @Override
    protected Future<Void> boot(ReadOnlyKeyObject config) {
        String busPrefix = config.getString(BUS_PREFIX_CONFIG_KEY);
        String pingTimeout = config.getString(PING_TIMEOUT_KEY);
        if (!ConfigurationService.areValuesNonNullAndResolved(busPrefix, pingTimeout))
            return Future.failedFuture("Couldn't start the Vertx event bus due to invalid configuration");
        VertxInstance.getHttpRouter()
                .route("/" + busPrefix + "/*")
                .subRouter(SockJSHandler.create(VertxInstance.getVertx())
                        .bridge(new SockJSBridgeOptions()
                                        .setPingTimeout(config.getLong(PING_TIMEOUT_KEY)) // Should be higher than client WebSocketBusOptions.pingInterval (which is set to 30_000 at the time of writing this code)
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

        return Future.succeededFuture();
    }
}
