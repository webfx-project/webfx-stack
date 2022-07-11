// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.datascope {

    // Direct dependencies modules
    requires java.base;
    requires webfx.stack.com.serial;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.db.datascope;
    exports dev.webfx.stack.db.datascope.aggregate;
    exports dev.webfx.stack.db.datascope.schema;

    // Provided services
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.db.datascope.aggregate.AggregateScope.ProvidedSerialCodec;

}