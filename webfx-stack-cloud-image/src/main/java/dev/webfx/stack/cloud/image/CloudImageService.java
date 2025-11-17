package dev.webfx.stack.cloud.image;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.blob.Blob;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.cloud.image.spi.CloudImageProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class CloudImageService {

    private static CloudImageProvider getProvider() {
        return SingleServiceProvider.getProvider(CloudImageProvider.class, () -> ServiceLoader.load(CloudImageProvider.class));
    }

    public static Future<Void> readyFuture() {
        return getProvider().readyFuture();
    }

    public static boolean isReady() {
        return getProvider().isReady();
    }

    // Public API (authentication not required)

    public static String url(String source, int width, int height) {
        return getProvider().url(source, width, height);
    }

    public static Future<Boolean> exists(String id) {
        return getProvider().exists(id);
    }

    public static String urlPattern() {
        return getProvider().urlPattern();
    }

    // Private API (authentication required)

    public static Future<Void> upload(Blob blob, String id, boolean overwrite) {
        return getProvider().upload(blob, id, overwrite);
    }

    public static Future<Void> delete(String id, boolean invalidate) {
        return getProvider().delete(id, invalidate);
    }

}
