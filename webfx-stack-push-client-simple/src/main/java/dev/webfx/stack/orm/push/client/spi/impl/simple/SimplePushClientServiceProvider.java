package dev.webfx.stack.orm.push.client.spi.impl.simple;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.orm.push.client.spi.PushClientServiceProvider;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.com.bus.call.BusCallService;
import dev.webfx.stack.push.ClientPushBusAddressesSharedByBothClientAndServer;
import dev.webfx.stack.session.state.client.ClientSideStateSession;

/**
 * @author Bruno Salmon
 */
public final class SimplePushClientServiceProvider implements PushClientServiceProvider {

    @Override
    public Registration listenServerPushCalls() {
        String clientRunId = ClientSideStateSession.getInstance().getRunId();
        String clientBusCallServiceAddress = ClientPushBusAddressesSharedByBothClientAndServer.computeClientBusCallServiceAddress(clientRunId);
        Console.log("Listening push notification at " + clientBusCallServiceAddress);
        return BusCallService.listenBusEntryCalls(clientBusCallServiceAddress);
    }
}
