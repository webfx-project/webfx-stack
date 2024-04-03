package dev.webfx.stack.cloud.cloudinary;

/**
 * @author Bruno Salmon
 */
public interface Cloudinary {

    Uploader uploader();

    Url url();

    Search search();
}
