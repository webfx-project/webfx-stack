package dev.webfx.framework.client.services.push.spi;

import dev.webfx.platform.shared.services.bus.Registration;

/**
 * @author Bruno Salmon
 */
public interface PushClientServiceProvider {

    Registration listenServerPushCalls(Object pushClientId);

}
