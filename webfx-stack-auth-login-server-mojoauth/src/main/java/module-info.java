// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.login.server.mojoauth {

    // Direct dependencies modules
    requires java.sdk;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.stack.auth.authn;
    requires webfx.stack.auth.login;
    requires webfx.stack.push.server;
    requires webfx.stack.routing.router;
    requires webfx.stack.session;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.auth.login.spi.impl.mojoauth;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.auth.login.spi.impl.mojoauth.MojoAuthLoginApplicationJob;
    provides dev.webfx.stack.auth.login.spi.LoginServiceProvider with dev.webfx.stack.auth.login.spi.impl.mojoauth.MojoAuthLoginServiceProvider;

}