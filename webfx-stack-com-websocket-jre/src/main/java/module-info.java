// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.websocket.jre {

    // Direct dependencies modules
    requires org.java_websocket;
    requires webfx.platform.ast;
    requires webfx.platform.console;
    requires webfx.stack.com.websocket;

    // Exported packages
    exports dev.webfx.stack.com.websocket.spi.impl.jre;

    // Provided services
    provides dev.webfx.stack.com.websocket.spi.WebSocketServiceProvider with dev.webfx.stack.com.websocket.spi.impl.jre.JreWebSocketServiceProvider;

}