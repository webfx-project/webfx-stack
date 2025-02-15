package dev.webfx.stack.com.bus.spi.impl.json.client.websocket;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.Objects;
import dev.webfx.stack.com.bus.BusOptions;

/**
 * @author Bruno Salmon
 */
public final class WebSocketBusOptions extends BusOptions {

    private Boolean serverSSL;
    private String serverHost;
    private String serverPort;
    private String websocketSuffix;
    private Integer pingInterval;

    private ReadOnlyAstObject socketOptions;

    @Override
    public WebSocketBusOptions turnUnsetPropertiesToDefault() {
        serverSSL = Objects.coalesce(serverSSL, Boolean.FALSE);
        serverHost = Objects.coalesce(serverHost, "localhost");
        serverPort = Objects.coalesce(serverPort, "80");
        websocketSuffix = Objects.coalesce(websocketSuffix, "websocket");
        pingInterval = Objects.coalesce(pingInterval, 30_000);
        super.turnUnsetPropertiesToDefault();
        return this;
    }

    @Override
    public WebSocketBusOptions applyConfig(ReadOnlyAstObject config) {
        super.applyConfig(config);
        serverSSL = config.getBoolean("serverSSL");
        serverHost = config.getString("serverHost");
        serverPort = config.getString("serverPort");
        websocketSuffix = config.getString("websocketSuffix");
        pingInterval = config.getInteger("pingInterval");
        if (serverSSL == null)
            serverSSL = "80".equals(serverPort);
        socketOptions = config;
        return this;
    }

    public WebSocketBusOptions setServerHost(String serverHost) {
        this.serverHost = serverHost;
        return this;
    }

    public String getServerHost() {
        return serverHost;
    }

    public WebSocketBusOptions setServerSSL(Boolean serverSSL) {
        this.serverSSL = serverSSL;
        return this;
    }

    public Boolean isServerSSL() {
        return serverSSL;
    }

    public WebSocketBusOptions setServerPort(String serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    public String getServerPort() {
        return serverPort;
    }

    public WebSocketBusOptions setWebsocketSuffix(String websocketSuffix) {
        this.websocketSuffix = websocketSuffix;
        return this;
    }

    public String getWebsocketSuffix() {
        return websocketSuffix;
    }

    public WebSocketBusOptions setPingInterval(Integer pingInterval) {
        this.pingInterval = pingInterval;
        return this;
    }

    public int getPingInterval() {
        return pingInterval;
    }


    public ReadOnlyAstObject getSocketOptions() {
        return socketOptions;
    }

}
