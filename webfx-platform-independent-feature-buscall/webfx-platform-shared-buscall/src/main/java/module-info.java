// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.platform.shared.buscall {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires webfx.platform.shared.async;
    requires webfx.platform.shared.boot;
    requires webfx.platform.shared.bus;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.serial;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.platform.shared.services.buscall;
    exports dev.webfx.platform.shared.services.buscall.spi;

    // Used services
    uses dev.webfx.platform.shared.services.buscall.spi.BusCallEndpoint;

    // Provided services
    provides dev.webfx.platform.shared.services.boot.spi.ApplicationModuleBooter with dev.webfx.platform.shared.services.buscall.BusCallModuleBooter;
    provides dev.webfx.platform.shared.services.serial.spi.SerialCodec with dev.webfx.platform.shared.services.buscall.BusCallArgument.ProvidedSerialCodec, dev.webfx.platform.shared.services.buscall.BusCallResult.ProvidedSerialCodec, dev.webfx.platform.shared.services.buscall.SerializableAsyncResult.ProvidedSerialCodec;

}