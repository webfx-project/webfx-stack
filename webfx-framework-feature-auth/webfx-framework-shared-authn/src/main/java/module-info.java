// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.authn {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.framework.shared.services.authn;
    exports dev.webfx.framework.shared.services.authn.spi;

    // Used services
    uses dev.webfx.framework.shared.services.authn.spi.AuthenticationServiceProvider;

}