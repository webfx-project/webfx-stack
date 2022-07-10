// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.client.querypush.simple {

    // Direct dependencies modules
    requires webfx.framework.client.querypush;
    requires webfx.framework.shared.querypush;
    requires webfx.platform.shared.boot;
    requires webfx.platform.shared.bus;

    // Exported packages
    exports dev.webfx.stack.framework.client.jobs.querypush;

    // Provided services
    provides dev.webfx.platform.shared.services.boot.spi.ApplicationJob with dev.webfx.stack.framework.client.jobs.querypush.QueryPushClientJob;
    provides dev.webfx.stack.framework.shared.services.querypush.spi.QueryPushServiceProvider with dev.webfx.stack.framework.client.jobs.querypush.QueryPushClientServiceProvider;

}