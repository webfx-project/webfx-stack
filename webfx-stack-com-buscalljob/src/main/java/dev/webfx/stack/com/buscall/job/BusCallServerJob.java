package dev.webfx.stack.com.buscall.job;

/**
 * @author Bruno Salmon
 */

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.com.bus.Registrations;
import dev.webfx.stack.com.buscall.BusCallService;

public final class BusCallServerJob implements ApplicationJob {

    private final Registrations registrations = new Registrations();

    @Override
    public void onStart() {
        // At this stage of initialization, the bus call end points should be already registered
        // So now starting the BusCallService by listening entry calls
        Console.log("- Starting listening bus entry calls");
        registrations.add(BusCallService.listenBusEntryCalls());
    }

    @Override
    public void onStop() {
        registrations.unregister();
    }
}
