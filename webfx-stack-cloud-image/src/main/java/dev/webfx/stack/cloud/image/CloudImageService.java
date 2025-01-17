package dev.webfx.stack.cloud.image;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.blob.Blob;

/**
 * @author Bruno Salmon
 */
public interface CloudImageService {

    Future<Boolean> exists(String id);

    Future<Void> upload(Blob blob, String id, boolean overwrite);

    Future<Void> delete(String id, boolean invalidate);

    default String url(String source, int width, int height) {
        String urlPattern = urlPattern();
        if (urlPattern == null)
            throw new IllegalStateException("[CloudImageService] urlPattern is null");
        String url = urlPattern.replace(":source", source);
        if (width > 0)
            url = url.replace(":width", "" + width);
        else
            url = url.replace("/w_:width", ""); // temporary
        if (height > 0)
            url = url.replace(":height", "" + height);
        else
            url = url.replace("/h_:height", ""); // temporary

        //We add a random parameter to prevent the cache to display an old image
        url = url + "?t=" + System.currentTimeMillis();
        return url;
    }

    String urlPattern();

}
