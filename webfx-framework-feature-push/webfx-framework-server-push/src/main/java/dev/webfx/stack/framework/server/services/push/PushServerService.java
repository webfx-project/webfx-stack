package dev.webfx.stack.framework.server.services.push;

import dev.webfx.stack.framework.server.services.push.spi.PushServerServiceProvider;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class PushServerService {

    public static PushServerServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(PushServerServiceProvider.class, () -> ServiceLoader.load(PushServerServiceProvider.class));
    }

    public static <T> Future<T> callClientService(String serviceAddress, Object javaArgument, Bus bus, Object pushClientId) {
        return getProvider().callClientService(serviceAddress, javaArgument, bus, pushClientId);
    }

    public static Future pingPushClient(Bus bus, Object pushClientId) {
        return getProvider().pingPushClient(bus, pushClientId);
    }

    public static void addUnresponsivePushClientListener(UnresponsivePushClientListener listener) {
        getProvider().addUnresponsivePushClientListener(listener);
    }

    public static void removeUnresponsivePushClientListener(UnresponsivePushClientListener listener) {
        getProvider().removeUnresponsivePushClientListener(listener);
    }
}