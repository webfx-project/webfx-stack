// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.serial {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.com.serial;
    exports dev.webfx.stack.com.serial.spi;
    exports dev.webfx.stack.com.serial.spi.impl;

    // Used services
    uses dev.webfx.stack.com.serial.spi.SerialCodec;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.com.serial.SerialCodecModuleBooter;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.com.serial.spi.impl.ProvidedBatchSerialCodec;

}