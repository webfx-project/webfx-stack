// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.query {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.com.buscall;
    requires webfx.stack.com.serial;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.datasource;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.db.query;
    exports dev.webfx.stack.db.query.compression;
    exports dev.webfx.stack.db.query.compression.repeat;
    exports dev.webfx.stack.db.query.spi;
    exports dev.webfx.stack.db.query.spi.impl;

    // Used services
    uses dev.webfx.stack.db.query.spi.QueryServiceProvider;

    // Provided services
    provides dev.webfx.stack.com.buscall.spi.BusCallEndpoint with dev.webfx.stack.db.query.ExecuteQueryBusCallEndpoint, dev.webfx.stack.db.query.ExecuteQueryBatchBusCallEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.db.query.QueryArgument.ProvidedSerialCodec, dev.webfx.stack.db.query.QueryResult.ProvidedSerialCodec;

}