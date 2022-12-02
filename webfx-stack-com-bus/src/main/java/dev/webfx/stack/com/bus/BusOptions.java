package dev.webfx.stack.com.bus;


import dev.webfx.platform.util.Objects;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;

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

    public BusOptions applyConfig(ReadOnlyKeyObject config) {
        busPrefix = config.getString("busPrefix", busPrefix);
        clientBusAddressPrefix = config.getString("clientBusAddressPrefix", clientBusAddressPrefix);
        serverBusAddress = config.getString("serverBusAddress", serverBusAddress);
        return this;
    }

    public BusOptions setBusPrefix(String busPrefix) {
        this.busPrefix = busPrefix;
        return this;
    }

    public String getBusPrefix() {
        return busPrefix;
    }

    public BusOptions setClientBusAddressPrefix(String clientBusAddressPrefix) {
        this.clientBusAddressPrefix = clientBusAddressPrefix;
        return this;
    }

    public String getClientBusAddressPrefix() {
        return clientBusAddressPrefix;
    }

    public BusOptions setServerBusAddress(String serverBusAddress) {
        this.serverBusAddress = serverBusAddress;
        return this;
    }

    public String getServerBusAddress() {
        return serverBusAddress;
    }

}
