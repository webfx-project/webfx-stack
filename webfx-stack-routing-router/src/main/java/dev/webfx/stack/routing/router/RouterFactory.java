package dev.webfx.stack.routing.router;

import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.routing.router.spi.RouterFactoryProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public class RouterFactory {

    public static RouterFactoryProvider getProvider() {
        return SingleServiceProvider.getProvider(RouterFactoryProvider.class, () -> ServiceLoader.load(RouterFactoryProvider.class));
    }


    public static Router createRouter() {
        return getProvider().createRouter();
    }
}
