package dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl.jsinterop;

import dev.webfx.stack.cloud.cloudinary.Search;
import dev.webfx.stack.cloud.cloudinary.Uploader;
import dev.webfx.stack.cloud.cloudinary.Url;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * @author Bruno Salmon
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Cloudinary")
public interface JsCloudinary {

    JsUploader uploader();

    JsUrl url();

    JsSearch search();

}
