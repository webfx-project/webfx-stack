// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.query.buscall {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.util;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.com.serial;
    requires webfx.stack.db.query;

    // Exported packages
    exports dev.webfx.stack.db.query.buscall;
    exports dev.webfx.stack.db.query.buscall.serial;
    exports dev.webfx.stack.db.query.buscall.serial.compression;
    exports dev.webfx.stack.db.query.buscall.serial.compression.repeat;

    // Provided services
    provides dev.webfx.stack.com.bus.call.spi.BusCallEndpoint with dev.webfx.stack.db.query.buscall.ExecuteQueryMethodEndpoint, dev.webfx.stack.db.query.buscall.ExecuteQueryBatchMethodEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.db.query.buscall.serial.QueryArgumentSerialCodec, dev.webfx.stack.db.query.buscall.serial.QueryResultSerialCodec, dev.webfx.stack.db.query.buscall.serial.PairSerialCodec;

}