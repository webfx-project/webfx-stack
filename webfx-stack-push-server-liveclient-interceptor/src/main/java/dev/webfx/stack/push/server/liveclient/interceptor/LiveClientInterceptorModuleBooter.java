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
        ServerJsonBusStateManager.setClientLiveListener((runId, userId, clientVersion, pwa, clientProfile, backoffice, sessionFamilyId, ownerSessionId) -> {
            PushServerService.clientIsLive(runId);
            PushServerService.setClientMetadata(runId, userId, clientVersion, pwa, clientProfile, backoffice);
            // Kept apart from the metadata above on purpose: that describes a client for the /monitor page,
            // while this is a security routing fact — which family to reach when it is revoked — and has no
            // business turning up in a snapshot somebody browses. The owning session travels with it because
            // the runId is client-chosen and the family is not; see the listener's javadoc.
            PushServerService.setClientSessionFamily(runId, sessionFamilyId, ownerSessionId);
        });
    }
}
