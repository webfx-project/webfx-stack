// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires transitive webfx.platform.async;
    requires webfx.platform.service;
    requires webfx.stack.com.serial;

    // Exported packages
    exports dev.webfx.stack.authn;
    exports dev.webfx.stack.authn.serial;
    exports dev.webfx.stack.authn.spi;

    // Used services
    uses dev.webfx.stack.authn.spi.AuthenticationServiceProvider;

    // Provided services
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.authn.serial.UserClaimsSerialCodec, dev.webfx.stack.authn.serial.UsernamePasswordCredentialsSerialCodec;

}