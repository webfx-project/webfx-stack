package dev.webfx.stack.push.server.liveclient.interceptor;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.stack.com.bus.spi.impl.json.server.ServerJsonBusStateManager;
import dev.webfx.stack.push.server.PushServerService;
/**
 * @author Bruno Salmon
 */
public class LiveClientInterceptorModuleBooter implements ApplicationModuleBooter {

    @Override
    public String getModuleName() {
        return "webfx-stack-push-server-liveclient-interceptor";
    }

    @Override
    public int getBootLevel() {
        return APPLICATION_LAUNCH_LEVEL;
    }

    @Override
    public void bootModule() {
        ServerJsonBusStateManager.setClientLiveListener(PushServerService::clientIsLive);
    }
}
