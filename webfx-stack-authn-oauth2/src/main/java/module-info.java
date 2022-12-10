// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.oauth2 {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.async;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.authn.oauth2;
    exports dev.webfx.stack.authn.oauth2.spi;

    // Used services
    uses dev.webfx.stack.authn.oauth2.spi.OAuth2Provider;

}