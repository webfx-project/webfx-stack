// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.db.querypush.client.simple {

    // Direct dependencies modules
    requires webfx.platform.boot;
    requires webfx.stack.com.bus;
    requires webfx.stack.db.querypush;
    requires webfx.stack.db.querypush.client;
    requires webfx.stack.db.querypush.remote;

    // Exported packages
    exports dev.webfx.stack.db.querypush.client.simple;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationJob with dev.webfx.stack.db.querypush.client.simple.SimpleQueryPushClientJob;
    provides dev.webfx.stack.db.querypush.spi.QueryPushServiceProvider with dev.webfx.stack.db.querypush.client.simple.SimpleQueryPushClientServiceProvider;

}