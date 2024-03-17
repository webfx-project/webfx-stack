// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.login {

    // Direct dependencies modules
    requires transitive webfx.platform.async;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.authn.login;
    exports dev.webfx.stack.authn.login.spi;

    // Used services
    uses dev.webfx.stack.authn.login.spi.LoginServiceProvider;

}