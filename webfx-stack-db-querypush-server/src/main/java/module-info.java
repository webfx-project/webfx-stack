// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.querypush.server {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.async;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.platform.util;
    requires webfx.stack.com.bus;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.query;
    requires webfx.stack.db.querypush;
    requires webfx.stack.db.querypush.buscall;
    requires webfx.stack.db.submit;
    requires webfx.stack.db.submit.listener;
    requires webfx.stack.push.server;

    // Exported packages
    exports dev.webfx.stack.db.querypush.server;
    exports dev.webfx.stack.db.querypush.server.spi.impl;

    // Provided services
    provides dev.webfx.stack.db.submit.listener.SubmitListener with dev.webfx.stack.db.querypush.server.QueryPushServerService.ProvidedSubmitListener;

}