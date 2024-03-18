package dev.webfx.stack.com.bus.spi.impl.json.client.websocket;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Objects;
import dev.webfx.stack.com.bus.BusOptions;

/**
 * @author Bruno Salmon
 */
public final class WebSocketBusOptions extends BusOptions {

    public enum Protocol {
        WS,   // Web Socket protocol, to be used by non web applications (Jre, Android, iOS)
        HTTP  // HTTP protocol, to be used by web applications running in the browser (GWT, TeaVM)
    }

    private Protocol protocol;
    private Boolean serverSSL;
    private String serverHost;
    private String serverPort;

    private Integer pingInterval;

    private ReadOnlyAstObject socketOptions;

    @Override
    public WebSocketBusOptions turnUnsetPropertiesToDefault() {
        protocol = Objects.coalesce(protocol, Protocol.WS);
        serverSSL = Objects.coalesce(serverSSL, Boolean.FALSE);
        serverHost = Objects.coalesce(serverHost, "localhost");
        serverPort = Objects.coalesce(serverPort, "80");
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
        pingInterval = config.getInteger("pingInterval");
        String configProtocol = config.getString("protocol");
        if (configProtocol != null) {
            switch (configProtocol.toLowerCase()) {
                case "https":
                    serverSSL = true;
                case "http":
                    protocol = Protocol.HTTP;
                    break;
                case "wss":
                    serverSSL = true;
                case "ws":
                    protocol = Protocol.WS;
                    break;
            }
        }
        if (protocol == null) {
            Console.log("⚠️ Default WS protocol will be applied for WebSocket due to unrecognized protocol configuration value: " + configProtocol);
            protocol = Protocol.WS;
        }
        return this;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public WebSocketBusOptions setProtocol(Protocol protocol) {
        this.protocol = protocol;
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

    public WebSocketBusOptions setServerPort(String serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    public String getServerPort() {
        return serverPort;
    }

    public Boolean isServerSSL() {
        return serverSSL;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public WebSocketBusOptions setPingInterval(Integer pingInterval) {
        this.pingInterval = pingInterval;
        return this;
    }

    public ReadOnlyAstObject getSocketOptions() {
        return socketOptions;
    }

}
