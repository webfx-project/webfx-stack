package dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.file.File;
import dev.webfx.stack.cloud.cloudinary.Uploader;
import dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl.jsinterop.JsUploader;

import java.util.Map;

/**
 * @author Bruno Salmon
 */
final class GwtJ2clUploader implements Uploader {

    private final JsUploader jsUploader;

    public GwtJ2clUploader(JsUploader jsUploader) {
        this.jsUploader = jsUploader;
    }

    @Override
    public Future<Map> upload(File file, String publicId, boolean overwrite) {
        return jsUploader.upload(file, publicId, overwrite);
    }

    @Override
    public Future<Map> destroy(String publicId, boolean invalidate) {
        return jsUploader.destroy(publicId, invalidate);
    }
}
