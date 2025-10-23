// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.serial {

    // Direct dependencies modules
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.reflect;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.com.serial;
    exports dev.webfx.stack.com.serial.spi;
    exports dev.webfx.stack.com.serial.spi.impl;
    exports dev.webfx.stack.com.serial.spi.impl.ast;
    exports dev.webfx.stack.com.serial.spi.impl.time;

    // Used services
    uses dev.webfx.stack.com.serial.spi.SerialCodec;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.com.serial.SerialCodecModuleBooter;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.com.serial.spi.impl.ProvidedBatchSerialCodec;

}