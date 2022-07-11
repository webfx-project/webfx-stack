package dev.webfx.stack.framework.client.services.push.spi;

import dev.webfx.stack.com.bus.Registration;

/**
 * @author Bruno Salmon
 */
public interface PushClientServiceProvider {

    Registration listenServerPushCalls(Object pushClientId);

}
