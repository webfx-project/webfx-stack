// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.auth.authn.buscall {

    // Direct dependencies modules
    requires webfx.stack.auth.authn;
    requires webfx.stack.com.bus.call;

    // Exported packages
    exports dev.webfx.stack.auth.authn.buscall;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.auth.authn.buscall.AuthenticateMethodEndpoint, dev.webfx.stack.auth.authn.buscall.GetUserClaimsMethodEndpoint;

}