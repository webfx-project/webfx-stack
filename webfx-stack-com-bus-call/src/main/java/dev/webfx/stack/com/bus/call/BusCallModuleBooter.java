package dev.webfx.stack.com.bus.call;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.bus.call.spi.BusCallEndpoint;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class BusCallModuleBooter implements ApplicationModuleBooter {

    @Override
    public String getModuleName() {
        return "webfx-stack-com-bus-call";
    }

    @Override
    public int getBootLevel() {
        return JOBS_START_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        StringBuilder sb = new StringBuilder();
        List<BusCallEndpoint> endpoints = Collections.listOf(ServiceLoader.load(BusCallEndpoint.class));
        endpoints.sort(Comparator.comparing(BusCallEndpoint::getAddress));
        for (BusCallEndpoint<?, ?> endpoint : endpoints) {
            BusCallService.registerBusCallEndpoint(endpoint);
            sb.append(sb.length() == 0 ? endpoints.size() + " endpoints provided for addresses:\n - " : "\n - ").append(toText(endpoint));
        }
        Console.log(sb);
        // Initializing the bus immediately to make the connection process happen while the application is initializing
        BusService.bus(); // Instantiating the bus (if not already done) is enough to open the connection
    }

    private static String toText(BusCallEndpoint endpoint) {
        String functionClassName = endpoint.toAsyncFunction().getClass().getName();
        int p = functionClassName.indexOf('$');
        if (p > 0)
            functionClassName = functionClassName.substring(0, p);
        return endpoint.getAddress() + " -> " + functionClassName;
    }
}
