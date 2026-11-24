package dev.webfx.stack.com.bus.call;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.meta.Meta;
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
        return COMMUNICATION_REGISTER_BOOT_LEVEL;
    }

    /**
     * The endpoints a command-line program has not registered yet; null once registered, and null throughout in an
     * application, which registers them at boot as it always has.
     */
    private static List<BusCallEndpoint> deferredEndpoints;

    @Override
    public void bootModule() {
        List<BusCallEndpoint> endpoints = Collections.listOf(ServiceLoader.load(BusCallEndpoint.class));
        endpoints.sort(Comparator.comparing(BusCallEndpoint::getAddress));
        boolean deferred = Meta.isCLI();
        StringBuilder sb = new StringBuilder();
        for (BusCallEndpoint<?, ?> endpoint : endpoints)
            sb.append(sb.length() == 0 ? endpoints.size() + " endpoints provided for addresses:\n - " : "\n - ").append(toText(endpoint));
        if (deferred && sb.length() > 0)
            sb.append("\n(registered on first use, this being a command-line program)");
        log(sb.toString());

        // Registering an endpoint registers a handler on the bus, and instantiating the bus is enough to open the
        // connection — so the loop below is itself what connects, and the explicit BusService.bus() after it has only
        // ever mattered for an application providing no endpoint at all.
        //
        // An application wants exactly that: the connection process runs while the rest of it initializes. A
        // command-line program does not. Most of its commands never reach the server — a help screen, a catalogue, a
        // look at what is cached — and connecting on their behalf dials whatever host the configuration happens to
        // default to and prints a "Connection refused" the command never asked for.
        //
        // So for a CLI the endpoints are kept aside instead, and registerDeferredEndpoints() puts them in place when a
        // command turns out to need the server. Nothing is skipped: the state once connected is the same, reached at
        // the job's boot level rather than this one, which for a one-shot process is a few dozen milliseconds later and
        // after the command has worked out which server it was asked about.
        if (deferred) {
            deferredEndpoints = endpoints;
            return;
        }
        registerEndpoints(endpoints);
        BusService.bus(); // Instantiating the bus (if not already done) is enough to open the connection
    }

    /**
     * Registers the endpoints that {@link #bootModule()} kept aside for a command-line program, and so opens the bus.
     * Call it before the first bus call of a command that needs the server; it does nothing in an application, or on a
     * second call.
     *
     * <p>It logs nothing, unlike the boot: the boot log already listed the endpoints when it set them aside, and by the
     * time this runs the boot's log buffer has been flushed, so anything written here would print whether or not the
     * boot was asked to be verbose.
     */
    public static void registerDeferredEndpoints() {
        List<BusCallEndpoint> endpoints = deferredEndpoints;
        if (endpoints == null) // an application, or already registered
            return;
        deferredEndpoints = null; // before registering, so that a re-entrant call cannot register them twice
        registerEndpoints(endpoints);
    }

    private static void registerEndpoints(List<BusCallEndpoint> endpoints) {
        for (BusCallEndpoint<?, ?> endpoint : endpoints)
            BusCallService.registerBusCallEndpoint(endpoint);
    }

    private static String toText(BusCallEndpoint endpoint) {
        String functionClassName = endpoint.toAsyncFunction().getClass().getName();
        int p = functionClassName.indexOf('$');
        if (p > 0)
            functionClassName = functionClassName.substring(0, p);
        return endpoint.getAddress() + " -> " + functionClassName;
    }
}
