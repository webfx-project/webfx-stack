// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.authz.server {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.util;
    requires webfx.stack.session.state.server;

    // Exported packages
    exports dev.webfx.stack.auth.authz.server;
    exports dev.webfx.stack.auth.authz.server.spi;

    // Used services
    uses dev.webfx.stack.auth.authz.server.spi.AuthorizationServerServiceProvider;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.auth.authz.server.AuthorizationServerJob;

}