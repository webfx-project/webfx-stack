package dev.webfx.stack.cloud.image.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.blob.Blob;

/**
 * @author Bruno Salmon
 */
public interface CloudImageProvider {

    default boolean isReady() {
        return urlPattern() != null;
    }

    Future<Void> readyFuture();

    // Public API (authentication not required)

    String urlPattern();

    default String url(String source, int width, int height) {
        String urlPattern = urlPattern();
        if (urlPattern == null)
            throw new IllegalStateException("[CloudImageService] urlPattern is null");
        // Note: if with < 0 or height < 0, this method may require correction
        String url = urlPattern
            .replace(":source", source)
            .replace(":width", String.valueOf(width))
            .replace(":height", String.valueOf(height));

        // We add a random parameter to prevent the cache to display an old image
        url = url + (url.contains("?") ? "&t=" : "?t=") + System.currentTimeMillis();
        return url;
    }

    Future<Boolean> exists(String id);

    // Private API (authentication required)

    Future<Void> upload(Blob blob, String id, boolean overwrite);

    Future<Void> delete(String id, boolean invalidate);

}
