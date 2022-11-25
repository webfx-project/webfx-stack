// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login.server.mojoauth {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.stack.authn;
    requires webfx.stack.authn.login;
    requires webfx.stack.push.server;
    requires webfx.stack.routing.router;
    requires webfx.stack.session;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.authn.login.spi.impl.mojoauth;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.authn.login.spi.impl.mojoauth.MojoAuthLoginApplicationJob;
    provides dev.webfx.stack.authn.login.spi.LoginServiceProvider with dev.webfx.stack.authn.login.spi.impl.mojoauth.MojoAuthLoginServiceProvider;

}