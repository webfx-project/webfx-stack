// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.entity.message.serial {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.stack.com.serial;
    requires webfx.stack.orm.entity;

    // Exported packages
    exports dev.webfx.stack.orm.entity.message.serial;

    // Provided services
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.orm.entity.message.serial.EntityIdImplSerialCodec, dev.webfx.stack.orm.entity.message.serial.EntityResultImplSerialCodec;

}