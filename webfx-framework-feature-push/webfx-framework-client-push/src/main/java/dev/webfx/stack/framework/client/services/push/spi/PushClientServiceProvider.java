package dev.webfx.stack.framework.client.services.push.spi;

import dev.webfx.stack.platform.shared.services.bus.Registration;

/**
 * @author Bruno Salmon
 */
public interface PushClientServiceProvider {

    Registration listenServerPushCalls(Object pushClientId);

}
