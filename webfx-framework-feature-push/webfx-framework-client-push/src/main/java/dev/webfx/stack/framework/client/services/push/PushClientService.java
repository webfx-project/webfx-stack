package dev.webfx.stack.framework.client.services.push;

import dev.webfx.platform.console.Console;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import dev.webfx.stack.framework.client.services.push.spi.PushClientServiceProvider;
import dev.webfx.stack.framework.shared.services.push.ClientPushBusAddressesSharedByBothClientAndServer;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.com.buscall.BusCallService;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * @author Bruno Salmon
 */
public final class PushClientService {

    private final static ObjectProperty<Object> pushClientIdProperty = new SimpleObjectProperty<>();

    static {
        // Registering the client push ping listener. This registration is private (ie just done locally on the client
        // event bus) so not directly visible from the server event bus but the server can reach that listener by calling
        // PushServerService.pingPushClient() because the client bus call service will finally pass the arg to that
        // listener over the local client bus.
        registerPushFunction(ClientPushBusAddressesSharedByBothClientAndServer.PUSH_PING_CLIENT_LISTENER_SERVICE_ADDRESS, arg -> {
            Console.log(arg);
            return "OK";
        });
        // But to make this work, the client bus call service must listen server calls. This should be done by calling:
        // PushClientService.listenServerPushCalls() as soon as the push client id has been generated.
    }

    public static Object getPushClientId() {
        return pushClientIdProperty.get();
    }

    public static ObjectProperty<Object> pushClientIdProperty() {
        return pushClientIdProperty;
    }

    public static PushClientServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(PushClientServiceProvider.class, () -> ServiceLoader.load(PushClientServiceProvider.class));
    }

    public static Registration listenServerPushCalls(Object pushClientId) {
        pushClientIdProperty.setValue(pushClientId);
        return getProvider().listenServerPushCalls(pushClientId);
    }

    public static <A, R> Registration registerPushFunction(String address, Function<A, R> javaFunction) {
        return BusCallService.registerBusCallEndpoint(address, javaFunction);
    }

}
