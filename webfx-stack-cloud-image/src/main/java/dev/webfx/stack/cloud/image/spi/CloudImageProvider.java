package dev.webfx.stack.cloud.image.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.blob.Blob;

/**
 * @author Bruno Salmon
 */
public abstract class CloudImageProvider {

    public boolean isReady() {
        return urlPattern() != null;
    }

    public abstract Future<Void> readyFuture();

    // Public API (authentication not required)

    public abstract String urlPattern();

    // This method is made final on purpose, to ensure it behaves the same for all implementations (ex: same behavior
    // with or without the REST layer).
    public final String url(String source, int width, int height) {
        String urlPattern = urlPattern();
        if (urlPattern == null)
            throw new IllegalStateException("[CloudImageService] urlPattern is null");
        // Note: if with < 0 or height < 0, this method may require correction
        String url = urlPattern
            .replace(":sourceWithImageExtension", sourceWithImageExtension(source))
            .replace(":source", source)
            .replace(":width", String.valueOf(width))
            .replace(":height", String.valueOf(height))
            // A timestamp parameter can help to prevent the cache from displaying an old image
            .replace(":timestamp", String.valueOf(System.currentTimeMillis()));
        while (true) {
            int p = url.indexOf("(");
            if (p == -1)
                break;
            int q = p + 1;
            while (url.charAt(q) != ')')
                q++;
            // if width = -1 or height = -1, we remove the parenthesis and the content inside
            if (url.charAt(q - 2) == '-' && url.charAt(q - 1) == '1')
                url = url.substring(0, p) + url.substring(q + 1);
            else // otherwise we just remove the parenthesis, not the content inside
                url = url.substring(0, p) + url.substring(p + 1, q) + url.substring(q + 1);
        }
        return url.replace("?&", "?");
    }

    public abstract Future<Boolean> exists(String id);

    // Private API (authentication required)

    public abstract Future<Void> upload(Blob blob, String id, boolean overwrite);

    public abstract Future<Void> delete(String id, boolean invalidate);

    /**
     * Ensures the source has an image extension. If not, .png is added by default
     */
    protected String sourceWithImageExtension(String source) {
        // No change if an image extension is present
        if (source.matches(".*\\.(jpg|jpeg|gif|webp|svg|bmp|tiff)$")) {
            return source;
        }
        // Adding .png if no extension is present (OpenJFX doesn't support .webp)
        return source + ".png";
    }

}
