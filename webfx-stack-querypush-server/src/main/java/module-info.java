// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.querypush.server {

    // Direct dependencies modules
    requires java.base;
    requires webfx.platform.console;
    requires webfx.platform.scheduler;
    requires webfx.platform.util;
    requires webfx.stack.async;
    requires webfx.stack.com.bus;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.query;
    requires webfx.stack.db.submit;
    requires webfx.stack.db.submitlistener;
    requires webfx.stack.push.server;
    requires webfx.stack.querypush;

    // Exported packages
    exports dev.webfx.stack.querypush.server;
    exports dev.webfx.stack.querypush.server.spi.impl;

    // Provided services
    provides dev.webfx.stack.db.submitlistener.SubmitListener with dev.webfx.stack.querypush.server.QueryPushServerService.ProvidedSubmitListener;

}