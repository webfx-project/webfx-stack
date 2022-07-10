// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.shared.query {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.buscall;
    requires webfx.platform.shared.datascope;
    requires webfx.platform.shared.datasource;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.serial;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.stack.platform.shared.services.query;
    exports dev.webfx.stack.platform.shared.services.query.compression;
    exports dev.webfx.stack.platform.shared.services.query.compression.repeat;
    exports dev.webfx.stack.platform.shared.services.query.spi;
    exports dev.webfx.stack.platform.shared.services.query.spi.impl;

    // Used services
    uses dev.webfx.stack.platform.shared.services.query.spi.QueryServiceProvider;

    // Provided services
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.platform.shared.services.query.QueryArgument.ProvidedSerialCodec, dev.webfx.stack.platform.shared.services.query.QueryResult.ProvidedSerialCodec;
    provides dev.webfx.stack.platform.shared.services.buscall.spi.BusCallEndpoint with dev.webfx.stack.platform.shared.services.query.ExecuteQueryBusCallEndpoint, dev.webfx.stack.platform.shared.services.query.ExecuteQueryBatchBusCallEndpoint;

}