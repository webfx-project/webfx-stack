package dev.webfx.stack.push.server;

import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.push.server.spi.PushServerServiceProvider;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class PushServerService {

    public static PushServerServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(PushServerServiceProvider.class, () -> ServiceLoader.load(PushServerServiceProvider.class));
    }

    public static <T> Future<T> push(String clientServiceAddress, Object javaArgument, DeliveryOptions options, Object clientRunId) {
        return push(clientServiceAddress, javaArgument, options, null, clientRunId);
    }

    public static <T> Future<T> push(String clientServiceAddress, Object javaArgument, DeliveryOptions options, Bus bus, Object clientRunId) {
        return getProvider().push(clientServiceAddress, javaArgument, options, bus, clientRunId);
    }

    public static Future<Void> pushState(Object state, Object clientRunId) {
        return pushState(state, null, clientRunId);
    }

    public static Future<Void> pushState(Object state, Bus bus, Object clientRunId) {
        return getProvider().pushPing(new DeliveryOptions().setState(state), bus, clientRunId);
    }

    public static void addUnresponsivePushClientListener(UnresponsivePushClientListener listener) {
        getProvider().addUnresponsivePushClientListener(listener);
    }

    public static void removeUnresponsivePushClientListener(UnresponsivePushClientListener listener) {
        getProvider().removeUnresponsivePushClientListener(listener);
    }
}