// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.oauth2.vertx {

    // Direct dependencies modules
    requires io.vertx.auth.oauth2;
    requires io.vertx.core;
    requires webfx.platform.async;
    requires webfx.platform.vertx.common;
    requires webfx.stack.authn.oauth2;

    // Exported packages
    exports dev.webfx.stack.authn.oauth2.spi.impl.vertx;

    // Provided services
    provides dev.webfx.stack.authn.oauth2.spi.OAuth2Provider with dev.webfx.stack.authn.oauth2.spi.impl.vertx.VertxOAuth2Provider;

}