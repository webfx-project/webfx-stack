// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.entity.messaging {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.util;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.serial;
    requires webfx.stack.orm.entity;
    requires webfx.stack.orm.entity.binding;
    requires webfx.stack.session.state.client.fx;

    // Exported packages
    exports dev.webfx.stack.orm.entity.messaging;
    exports dev.webfx.stack.orm.entity.messaging.serial;

    // Provided services
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.orm.entity.messaging.serial.EntityIdImplSerialCodec, dev.webfx.stack.orm.entity.messaging.serial.EntityResultImplSerialCodec;

}