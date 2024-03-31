package dev.webfx.stack.orm.push.client;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.com.bus.call.BusCallService;
import dev.webfx.stack.orm.push.client.spi.PushClientServiceProvider;
import dev.webfx.stack.push.ClientPushBusAddressesSharedByBothClientAndServer;
import dev.webfx.stack.session.state.client.fx.FXConnected;

import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class PushClientService {

    static {
        // Registering the client push ping listener. This registration is private (ie just done locally on the client
        // event bus) so not directly visible from the server event bus but the server can reach that listener by calling
        // PushServerService.pingPushClient() because the client bus call service will finally pass the arg to that
        // listener over the local client bus.
        registerPushFunction(ClientPushBusAddressesSharedByBothClientAndServer.PUSH_PING_CLIENT_LISTENER_SERVICE_ADDRESS, arg -> {
            Console.log("Received server push: " + arg);
            return (Void) null;
        });
        // But to make this work, the client bus call service must listen server calls! This takes place as soon as the
        // connection to the server is ready, or each time we reconnect to the server:
        FXConnected.runOnEachConnected(getProvider()::listenServerPushCalls); // Register this client over the event bus for push notifications
    }

    public static PushClientServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(PushClientServiceProvider.class, () -> ServiceLoader.load(PushClientServiceProvider.class));
    }

    public static <A, R> Registration registerPushFunction(String address, Function<A, R> javaFunction) {
        return BusCallService.registerBusCallEndpoint(address, javaFunction);
    }

}
