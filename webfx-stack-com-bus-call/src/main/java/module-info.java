// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.com.bus.call {

    // Direct dependencies modules
    requires javafx.base;
    requires webfx.platform.ast;
    requires webfx.platform.async;
    requires webfx.platform.boot;
    requires webfx.platform.console;
    requires webfx.platform.util;
    requires webfx.stack.com.bus;
    requires webfx.stack.com.serial;
    requires webfx.stack.session.state;

    // Exported packages
    exports dev.webfx.stack.com.bus.call;
    exports dev.webfx.stack.com.bus.call.spi;

    // Used services
    uses dev.webfx.stack.com.bus.call.spi.BusCallEndpoint;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.com.bus.call.BusCallModuleBooter;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.com.bus.call.BusCallArgument.ProvidedSerialCodec, dev.webfx.stack.com.bus.call.BusCallResult.ProvidedSerialCodec, dev.webfx.stack.com.bus.call.SerializableAsyncResult.ProvidedSerialCodec;

}