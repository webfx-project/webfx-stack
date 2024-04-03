package dev.webfx.stack.cloud.cloudinary;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.file.File;

import java.util.Map;

/**
 * @author Bruno Salmon
 */
public interface Uploader {

    Future<Map> upload(File file, String publicId, boolean overwrite);

    Future<Map> destroy(String publicId, boolean invalidate);

}
