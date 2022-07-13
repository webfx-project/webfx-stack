// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.querypush.client.simple {

    // Direct dependencies modules
    requires webfx.platform.boot;
    requires webfx.stack.com.bus;
    requires webfx.stack.querypush;
    requires webfx.stack.querypush.client;

    // Exported packages
    exports dev.webfx.stack.querypush.client.simple;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.querypush.client.simple.SimpleQueryPushClientJob;
    provides dev.webfx.stack.querypush.spi.QueryPushServiceProvider with dev.webfx.stack.querypush.client.simple.SimpleQueryPushClientServiceProvider;

}