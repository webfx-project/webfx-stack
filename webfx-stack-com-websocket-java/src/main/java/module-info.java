// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.websocket.java {

    // Direct dependencies modules
    requires Java.WebSocket;
    requires java.base;
    requires webfx.platform.shared.log;
    requires webfx.stack.com.websocket;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.com.websocket.spi.impl.java;

    // Provided services
    provides dev.webfx.stack.com.websocket.spi.WebSocketServiceProvider with dev.webfx.stack.com.websocket.spi.impl.java.JavaWebSocketServiceProvider;

}