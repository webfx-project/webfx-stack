// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.server.querypush {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.server.push;
    requires webfx.framework.shared.querypush;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.scheduler;
    requires webfx.platform.shared.util;
    requires webfx.stack.async;
    requires webfx.stack.com.bus;
    requires webfx.stack.db.datascope;
    requires webfx.stack.db.query;
    requires webfx.stack.db.submit;
    requires webfx.stack.db.submitlistener;

    // Exported packages
    exports dev.webfx.stack.framework.server.services.querypush;
    exports dev.webfx.stack.framework.server.services.querypush.spi.impl;

    // Provided services
    provides dev.webfx.stack.db.submitlistener.SubmitListener with dev.webfx.stack.framework.server.services.querypush.QueryPushServerService.ProvidedSubmitListener;

}