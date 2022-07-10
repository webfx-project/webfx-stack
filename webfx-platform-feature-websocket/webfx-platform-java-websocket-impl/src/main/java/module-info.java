// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.java.websocket.impl {

    // Direct dependencies modules
    requires Java.WebSocket;
    requires java.base;
    requires webfx.platform.client.websocket;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.log;

    // Exported packages
    exports dev.webfx.stack.platform.websocket.spi.impl.java;

    // Provided services
    provides dev.webfx.stack.platform.websocket.spi.WebSocketServiceProvider with dev.webfx.stack.platform.websocket.spi.impl.java.JavaWebSocketServiceProvider;

}