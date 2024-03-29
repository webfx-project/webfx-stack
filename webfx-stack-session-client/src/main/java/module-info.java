// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.session.client {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.async;
    requires webfx.platform.storage;
    requires webfx.platform.util;
    requires webfx.stack.com.serial;
    requires webfx.stack.session;

    // Exported packages
    exports dev.webfx.stack.session.spi.impl.client;

    // Provided services
    provides dev.webfx.stack.session.spi.SessionServiceProvider with dev.webfx.stack.session.spi.impl.client.ClientSessionServiceProvider;

}