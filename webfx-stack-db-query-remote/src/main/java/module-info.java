// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.query.remote {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.db.query;
    requires webfx.stack.db.query.buscall;

    // Exported packages
    exports dev.webfx.stack.db.query.spi.impl.remote;

    // Provided services
    provides dev.webfx.stack.db.query.spi.QueryServiceProvider with dev.webfx.stack.db.query.spi.impl.remote.RemoteQueryServiceProvider;

}