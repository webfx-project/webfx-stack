// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.shared.serial {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.boot;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.stack.com.serial;
    exports dev.webfx.stack.com.serial.spi;
    exports dev.webfx.stack.com.serial.spi.impl;

    // Used services
    uses dev.webfx.stack.com.serial.spi.SerialCodec;

    // Provided services
    provides dev.webfx.platform.shared.services.boot.spi.ApplicationModuleBooter with dev.webfx.stack.com.serial.SerialCodecModuleBooter;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.com.serial.spi.impl.ProvidedBatchSerialCodec;

}