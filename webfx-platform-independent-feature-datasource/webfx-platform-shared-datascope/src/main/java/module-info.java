// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.shared.datascope {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.serial;

    // Exported packages
    exports dev.webfx.stack.platform.shared.datascope;
    exports dev.webfx.stack.platform.shared.datascope.aggregate;
    exports dev.webfx.stack.platform.shared.datascope.schema;

    // Provided services
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.platform.shared.datascope.aggregate.AggregateScope.ProvidedSerialCodec;

}