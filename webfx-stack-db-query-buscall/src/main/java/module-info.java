// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.query.buscall {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.db.query;

    // Exported packages
    exports dev.webfx.stack.db.query.buscall;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.db.query.buscall.ExecuteQueryMethodEndpoint, dev.webfx.stack.db.query.buscall.ExecuteQueryBatchMethodEndpoint, dev.webfx.stack.db.query.buscall.LoadClassMappingsBusCallEndpoint;

}