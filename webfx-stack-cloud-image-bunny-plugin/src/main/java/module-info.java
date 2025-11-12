// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.cloud.image.bunny.plugin {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.blob;
    requires webfx.platform.conf;
    requires webfx.platform.console;
    requires webfx.platform.fetch;
    requires webfx.platform.util.http;
    requires webfx.stack.cloud.image;

    // Exported packages
    exports dev.webfx.stack.cloud.image.impl.bunny;

    // Provided services
    provides dev.webfx.stack.cloud.image.spi.CloudImageProvider with dev.webfx.stack.cloud.image.impl.bunny.BunnyImageProvider;

}