// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.authn {

    // Direct dependencies modules
    requires java.base;
    requires transitive webfx.platform.async;
    requires webfx.platform.json;
    requires webfx.platform.util;
    requires webfx.stack.com.serial;

    // Exported packages
    exports dev.webfx.stack.auth.authn;
    exports dev.webfx.stack.auth.authn.serial;
    exports dev.webfx.stack.auth.authn.spi;

    // Used services
    uses dev.webfx.stack.auth.authn.spi.AuthenticationServiceProvider;

    // Provided services
    provides dev.webfx.stack.com.serial.spi.impl.SerialCodecBase with dev.webfx.stack.auth.authn.serial.UserClaimsSerialCodec, dev.webfx.stack.auth.authn.serial.UsernamePasswordCredentialSerialCodec;

}