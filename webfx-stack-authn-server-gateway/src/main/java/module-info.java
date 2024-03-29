// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.server.gateway {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.async;
    requires webfx.stack.authn;
    requires webfx.stack.authn.logout.server;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.authn.server.gateway.spi;
    exports dev.webfx.stack.authn.server.gateway.spi.impl;

}