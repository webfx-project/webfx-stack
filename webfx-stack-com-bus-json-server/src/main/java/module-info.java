// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.json.server {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.json;
    requires webfx.stack.session;
    requires webfx.stack.session.state;
    requires webfx.stack.session.state.server;

    // Exported packages
    exports dev.webfx.stack.com.bus.spi.impl.json.server;

}