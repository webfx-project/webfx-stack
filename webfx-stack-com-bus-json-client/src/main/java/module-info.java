// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.json.client {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.bus.client;
    requires webfx.stack.com.bus.json;
    requires webfx.stack.session.state;
    requires webfx.stack.session.state.client;

    // Exported packages
    exports dev.webfx.stack.com.bus.spi.impl.json.client;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.com.bus.spi.impl.json.client.JsonClientBusModuleBooter;

}