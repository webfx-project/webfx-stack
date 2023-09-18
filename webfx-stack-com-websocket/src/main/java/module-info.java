// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.websocket {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.ast;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.com.websocket;
    exports dev.webfx.stack.com.websocket.spi;

    // Used services
    uses dev.webfx.stack.com.websocket.spi.WebSocketServiceProvider;

}