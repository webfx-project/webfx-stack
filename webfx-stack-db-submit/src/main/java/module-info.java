// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.submit {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.com.buscall;
    requires webfx.stack.com.serial;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.datasource;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.db.submit;
    exports dev.webfx.stack.db.submit.spi;
    exports dev.webfx.stack.db.submit.spi.impl;

    // Used services
    uses dev.webfx.stack.db.submit.spi.SubmitServiceProvider;

    // Provided services
    provides dev.webfx.stack.com.buscall.spi.BusCallEndpoint with dev.webfx.stack.db.submit.ExecuteSubmitBusCallEndpoint, dev.webfx.stack.db.submit.ExecuteSubmitBatchBusCallEndpoint;
    provides dev.webfx.stack.com.serial.spi.SerialCodec with dev.webfx.stack.db.submit.SubmitArgument.ProvidedSerialCodec, dev.webfx.stack.db.submit.SubmitResult.ProvidedSerialCodec, dev.webfx.stack.db.submit.GeneratedKeyBatchIndex.ProvidedSerialCodec;

}