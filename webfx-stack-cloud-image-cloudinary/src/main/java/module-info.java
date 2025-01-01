// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.cloud.image.cloudinary {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.blob;
    requires webfx.platform.conf;
    requires webfx.platform.fetch;
    requires webfx.platform.util;
    requires webfx.platform.util.http;
    requires webfx.stack.cloud.image;
    requires webfx.stack.hash.sha1;

    // Exported packages
    exports dev.webfx.stack.cloud.image.impl.cloudinary;

}