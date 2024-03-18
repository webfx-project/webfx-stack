package dev.webfx.stack.com.bus;


import dev.webfx.platform.util.Objects;
import dev.webfx.platform.ast.ReadOnlyAstObject;

/**
 * @author Bruno Salmon
 */
public class BusOptions {

    private String busPrefix;
    private String clientBusAddressPrefix;
    private String serverBusAddress;

    public BusOptions turnUnsetPropertiesToDefault() {
        busPrefix = Objects.coalesce(busPrefix, "eventbus");
        clientBusAddressPrefix = Objects.coalesce(clientBusAddressPrefix, "client");
        serverBusAddress = Objects.coalesce(serverBusAddress, "server");
        return this;
    }

    public BusOptions applyConfig(ReadOnlyAstObject config) {
        busPrefix = config.getString("busPrefix", busPrefix);
        clientBusAddressPrefix = config.getString("clientBusAddressPrefix", clientBusAddressPrefix);
        serverBusAddress = config.getString("serverBusAddress", serverBusAddress);
        return this;
    }

    public String getBusPrefix() {
        return busPrefix;
    }

}
