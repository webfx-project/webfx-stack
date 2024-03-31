// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.websocket {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.service;

    // Exported packages
    exports dev.webfx.stack.com.websocket;
    exports dev.webfx.stack.com.websocket.spi;

    // Used services
    uses dev.webfx.stack.com.websocket.spi.WebSocketServiceProvider;

}