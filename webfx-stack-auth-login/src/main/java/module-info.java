// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.login {

    // Direct dependencies modules
    requires java.base;
    requires transitive webfx.platform.async;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.auth.login;
    exports dev.webfx.stack.auth.login.spi;

    // Used services
    uses dev.webfx.stack.auth.login.spi.LoginServiceProvider;

}