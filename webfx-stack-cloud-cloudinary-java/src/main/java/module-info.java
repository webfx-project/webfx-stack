// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.cloud.cloudinary.java {

    // Direct dependencies modules
    requires cloudinary.core;
    requires webfx.platform.async;
    requires webfx.platform.file;
    requires webfx.platform.scheduler;
    requires webfx.stack.cloud.cloudinary;

    // Exported packages
    exports dev.webfx.stack.cloud.cloudinary.spi.impl.java;

    // Provided services
    provides dev.webfx.stack.cloud.cloudinary.spi.CloudinaryProvider with dev.webfx.stack.cloud.cloudinary.spi.impl.java.JavaCloudinaryProvider;

}