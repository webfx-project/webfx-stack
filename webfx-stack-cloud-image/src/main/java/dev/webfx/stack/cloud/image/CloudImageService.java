package dev.webfx.stack.cloud.image;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.file.File;

/**
 * @author Bruno Salmon
 */
public interface CloudImageService {

    Future<Boolean> exists(String id);

    Future<Void> upload(File file, String id, boolean overwrite);

    Future<Void> delete(String id, boolean invalidate);

    default String url(String source, int width, int height) {
        String url = urlPattern().replace(":source", source);
        if (width > 0)
            url = url.replace(":width", "" + width);
        else
            url = url.replace("/w_:width", ""); // temporary
        if (height > 0)
            url = url.replace(":height", "" + height);
        else
            url = url.replace("/h_:height", ""); // temporary
        return url;
    }

    String urlPattern();

}
