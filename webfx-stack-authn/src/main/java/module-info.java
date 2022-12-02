// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn {

    // Direct dependencies modules
    requires java.base;
    requires transitive webfx.platform.async;
    requires webfx.platform.json;
    requires webfx.platform.util;
    requires webfx.stack.com.serial;

    // Exported packages
    exports dev.webfx.stack.authn;
    exports dev.webfx.stack.authn.serial;
    exports dev.webfx.stack.authn.spi;

    // Used services
    uses dev.webfx.stack.authn.spi.AuthenticationServiceProvider;

    // Provided services
    provides dev.webfx.stack.com.serial.spi.impl.SerialCodecBase with dev.webfx.stack.authn.serial.UserClaimsSerialCodec, dev.webfx.stack.authn.serial.UsernamePasswordCredentialSerialCodec;

}