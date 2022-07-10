// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.client.websocket {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.stack.platform.websocket;
    exports dev.webfx.stack.platform.websocket.spi;

    // Used services
    uses dev.webfx.stack.platform.websocket.spi.WebSocketServiceProvider;

}