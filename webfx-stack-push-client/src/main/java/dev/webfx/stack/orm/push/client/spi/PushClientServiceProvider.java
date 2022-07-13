package dev.webfx.stack.orm.push.client.spi;

import dev.webfx.stack.com.bus.Registration;

/**
 * @author Bruno Salmon
 */
public interface PushClientServiceProvider {

    Registration listenServerPushCalls(Object pushClientId);

}
