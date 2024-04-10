package dev.webfx.stack.cloud.image;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.file.File;

import java.util.Map;

/**
 * @author Bruno Salmon
 */
public interface CloudImageService {

    Future<Boolean> exists(String publicId);

    Future<Map> upload(File file, String publicId, boolean overwrite);

    Future<Map> destroy(String publicId, boolean invalidate);

    String url(String source, int width, int height);

}
