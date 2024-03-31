// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authz.server {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.service;
    requires webfx.stack.session.state.server;

    // Exported packages
    exports dev.webfx.stack.authz.server;
    exports dev.webfx.stack.authz.server.spi;

    // Used services
    uses dev.webfx.stack.authz.server.spi.AuthorizationServerServiceProvider;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.authz.server.AuthorizationServerJob;

}