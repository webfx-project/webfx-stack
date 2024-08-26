// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.authn.buscall {

    // Direct dependencies modules
    requires webfx.stack.authn;
    requires webfx.stack.com.bus.call;

    // Exported packages
    exports dev.webfx.stack.authn.buscall;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.authn.buscall.AuthenticateMethodEndpoint, dev.webfx.stack.authn.buscall.GetUserClaimsMethodEndpoint, dev.webfx.stack.authn.buscall.UpdateCredentialsMethodEndpoint, dev.webfx.stack.authn.buscall.LogoutMethodEndpoint;

}