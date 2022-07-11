// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.submit.remote {

    // Direct dependencies modules
    requires webfx.stack.db.submit;

    // Exported packages
    exports dev.webfx.stack.db.submit.spi.impl.remote;

    // Provided services
    provides dev.webfx.stack.db.submit.spi.SubmitServiceProvider with dev.webfx.stack.db.submit.spi.impl.remote.RemoteSubmitServiceProvider;

}