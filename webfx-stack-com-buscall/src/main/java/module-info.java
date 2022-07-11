// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.buscall {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.platform.shared.boot;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.serial;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.com.buscall;
    exports dev.webfx.stack.com.buscall.spi;

    // Used services
    uses dev.webfx.stack.com.buscall.spi.BusCallEndpoint;

    // Provided services
    provides dev.webfx.platform.shared.services.boot.spi.ApplicationModuleBooter with dev.webfx.stack.com.buscall.BusCallModuleBooter;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.com.buscall.BusCallArgument.ProvidedSerialCodec, dev.webfx.stack.com.buscall.BusCallResult.ProvidedSerialCodec, dev.webfx.stack.com.buscall.SerializableAsyncResult.ProvidedSerialCodec;

}