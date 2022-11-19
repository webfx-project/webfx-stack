// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.push.server.liveclient.interceptor {

    // Direct dependencies modules
    requires webfx.platform.boot;
    requires webfx.stack.com.bus.json.server;
    requires webfx.stack.push.server;

    // Exported packages
    exports dev.webfx.stack.push.server.liveclient.interceptor;

    // Provided services
    provides dev.webfx.platform.boot.spi.ApplicationModuleBooter with dev.webfx.stack.push.server.liveclient.interceptor.LiveClientInterceptorModuleBooter;

}