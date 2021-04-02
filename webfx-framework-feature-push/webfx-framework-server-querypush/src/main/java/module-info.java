// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.server.querypush {

    // Direct dependencies modules
    requires java.base;
    requires webfx.framework.server.push;
    requires webfx.framework.shared.querypush;
    requires webfx.platform.shared.bus;
    requires webfx.platform.shared.datascope;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.query;
    requires webfx.platform.shared.scheduler;
    requires webfx.platform.shared.submit;
    requires webfx.platform.shared.submitlistener;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.framework.server.services.querypush;
    exports dev.webfx.framework.server.services.querypush.spi.impl;

    // Provided services
    provides dev.webfx.platform.server.services.submitlistener.SubmitListener with dev.webfx.framework.server.services.querypush.QueryPushServerService.ProvidedSubmitListener;

}