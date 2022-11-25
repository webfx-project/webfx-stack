// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.server {

    // Direct dependencies modules
    requires webfx.platform.boot;
    requires webfx.stack.authn;
    requires webfx.stack.session.state.server;

    // Exported packages
    exports dev.webfx.stack.authn.server;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.authn.server.AuthenticationServerJob;

}