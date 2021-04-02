// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.shared.submit.impl.remote {

    // Direct dependencies modules
    requires webfx.platform.shared.submit;

    // Exported packages
    exports dev.webfx.platform.shared.services.submit.spi.impl.remote;

    // Provided services
    provides dev.webfx.platform.shared.services.submit.spi.SubmitServiceProvider with dev.webfx.platform.shared.services.submit.spi.impl.remote.RemoteSubmitServiceProvider;

}