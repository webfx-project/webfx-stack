package dev.webfx.stack.framework.client.services.push.spi.impl.simple;

import dev.webfx.stack.framework.client.services.push.spi.PushClientServiceProvider;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.com.buscall.BusCallService;
import dev.webfx.platform.shared.services.log.Logger;
import dev.webfx.stack.framework.shared.services.push.ClientPushBusAddressesSharedByBothClientAndServer;

/**
 * @author Bruno Salmon
 */
public final class SimplePushClientServiceProvider implements PushClientServiceProvider {

    @Override
    public Registration listenServerPushCalls(Object pushClientId) {
        String clientBusCallServiceAddress = ClientPushBusAddressesSharedByBothClientAndServer.computeClientBusCallServiceAddress(pushClientId);
        Logger.log("Subscribing " + clientBusCallServiceAddress);
        return BusCallService.listenBusEntryCalls(clientBusCallServiceAddress);
    }
}
