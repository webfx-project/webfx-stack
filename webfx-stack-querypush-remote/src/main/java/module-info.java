// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.querypush.remote {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.stack.com.bus.call;
    requires webfx.stack.db.datasource;
    requires webfx.stack.querypush;
    requires webfx.stack.querypush.buscall;

    // Exported packages
    exports dev.webfx.stack.querypush.spi.impl.remote;

}