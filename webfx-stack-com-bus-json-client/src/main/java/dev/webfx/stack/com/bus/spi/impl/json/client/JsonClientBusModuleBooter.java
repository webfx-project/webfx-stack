package dev.webfx.stack.com.bus.spi.impl.json.client;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public class JsonClientBusModuleBooter implements ApplicationModuleBooter {

    private static boolean isCommunicationAllowed;
    private static List<JsonBus> pendingPingStateJsonBuses = new ArrayList<>();

    static boolean isCommunicationAllowed() {
        return isCommunicationAllowed;
    }

    static void registerPendingPingStateJsonBus(JsonBus jsonBus) {
        pendingPingStateJsonBuses.add(jsonBus);
    }

    @Override
    public String getModuleName() {
        return "webfx-stack-com-json-client";
    }

    @Override
    public int getBootLevel() {
        return COMMUNICATION_ALL_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        isCommunicationAllowed = true;
        for (JsonBus jsonBus : pendingPingStateJsonBuses)
            jsonBus.sendPingStateNow();
        pendingPingStateJsonBuses.clear();
        pendingPingStateJsonBuses = null;
    }
}
