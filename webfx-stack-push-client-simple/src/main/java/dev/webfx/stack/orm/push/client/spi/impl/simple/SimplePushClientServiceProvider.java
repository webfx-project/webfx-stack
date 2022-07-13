package dev.webfx.stack.orm.push.client.spi.impl.simple;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.orm.push.client.spi.PushClientServiceProvider;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.com.bus.call.BusCallService;
import dev.webfx.stack.push.ClientPushBusAddressesSharedByBothClientAndServer;

/**
 * @author Bruno Salmon
 */
public final class SimplePushClientServiceProvider implements PushClientServiceProvider {

    @Override
    public Registration listenServerPushCalls(Object pushClientId) {
        String clientBusCallServiceAddress = ClientPushBusAddressesSharedByBothClientAndServer.computeClientBusCallServiceAddress(pushClientId);
        Console.log("Subscribing " + clientBusCallServiceAddress);
        return BusCallService.listenBusEntryCalls(clientBusCallServiceAddress);
    }
}
