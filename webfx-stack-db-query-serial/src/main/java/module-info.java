// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.query.serial {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.util;
    requires webfx.stack.com.serial;
    requires webfx.stack.db.query;

    // Exported packages
    exports dev.webfx.stack.db.query.serial;
    exports dev.webfx.stack.db.query.serial.compression;
    exports dev.webfx.stack.db.query.serial.compression.repeat;

    // Provided services
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.db.query.serial.QueryArgumentSerialCodec, dev.webfx.stack.db.query.serial.QueryResultSerialCodec, dev.webfx.stack.db.query.serial.PairSerialCodec;

}