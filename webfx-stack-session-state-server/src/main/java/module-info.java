// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.session.state.server {

    // Direct dependencies modules
    requires transitive webfx.platform.async;
    requires webfx.platform.console;
    requires transitive webfx.platform.util;
    requires webfx.stack.authn.logout.server;
    requires webfx.stack.push.server;
    requires webfx.stack.session;
    requires webfx.stack.session.state;
    requires webfx.stack.session.token;

    // Exported packages
    exports dev.webfx.stack.session.state.server;

}