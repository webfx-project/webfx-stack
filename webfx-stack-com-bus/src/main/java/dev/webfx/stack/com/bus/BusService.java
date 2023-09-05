package dev.webfx.stack.com.bus;

import dev.webfx.stack.com.bus.spi.BusServiceProvider;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class BusService {

    public static BusServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(BusServiceProvider.class, () -> ServiceLoader.load(BusServiceProvider.class));
    }

    public static Bus bus() {
        return getProvider().bus();
    }


}
