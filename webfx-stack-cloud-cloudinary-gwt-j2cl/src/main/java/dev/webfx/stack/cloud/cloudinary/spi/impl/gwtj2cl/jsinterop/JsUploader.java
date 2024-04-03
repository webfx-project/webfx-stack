package dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl.jsinterop;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.file.File;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

import java.util.Map;

/**
 * @author Bruno Salmon
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Uploader")
public interface JsUploader {

    Future<Map> upload(File file, String publicId, boolean overwrite);

    Future<Map> destroy(String publicId, boolean invalidate);

}
