// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.cloud.cloudinary {

    // Direct dependencies modules
    requires webfx.platform.async;
    requires webfx.platform.file;
    requires webfx.platform.service;

    // Exported packages
    exports dev.webfx.stack.cloud.cloudinary;
    exports dev.webfx.stack.cloud.cloudinary.api;
    exports dev.webfx.stack.cloud.cloudinary.spi;

    // Used services
    uses dev.webfx.stack.cloud.cloudinary.spi.CloudinaryProvider;

}