// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.util;
    requires webfx.stack.async;

    // Exported packages
    exports dev.webfx.stack.authn;
    exports dev.webfx.stack.authn.spi;

    // Used services
    uses dev.webfx.stack.authn.spi.AuthenticationServiceProvider;

}