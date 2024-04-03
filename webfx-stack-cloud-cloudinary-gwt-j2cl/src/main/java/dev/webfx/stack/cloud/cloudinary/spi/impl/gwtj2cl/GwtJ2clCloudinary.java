package dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl;

import dev.webfx.stack.cloud.cloudinary.Cloudinary;
import dev.webfx.stack.cloud.cloudinary.Search;
import dev.webfx.stack.cloud.cloudinary.Uploader;
import dev.webfx.stack.cloud.cloudinary.Url;
import dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl.jsinterop.JsCloudinary;

/**
 * @author Bruno Salmon
 */
public final class GwtJ2clCloudinary implements Cloudinary {

    private final JsCloudinary jsCloudinary;

    public GwtJ2clCloudinary(String cloudName, String apiKey, String apiSecret) {
        jsCloudinary = null;
    }

    @Override
    public Uploader uploader() {
        return new GwtJ2clUploader(null);
    }

    @Override
    public Url url() {
        return new GwtJ2clUrl(null);
    }

    @Override
    public Search search() {
        return new GwtJ2clSearch(null);
    }

}
