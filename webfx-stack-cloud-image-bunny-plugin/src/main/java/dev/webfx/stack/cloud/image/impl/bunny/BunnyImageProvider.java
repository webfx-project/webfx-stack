package dev.webfx.stack.cloud.image.impl.bunny;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.blob.Blob;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.Fetch;
import dev.webfx.platform.fetch.FetchOptions;
import dev.webfx.platform.util.http.HttpMethod;
import dev.webfx.stack.cloud.image.spi.impl.CloudImageProviderBase;

/**
 * @author Bruno Salmon
 */
public final class BunnyImageProvider extends CloudImageProviderBase {

    private static final String CONFIG_PATH = "webfx.stack.cloud.image.bunny";

    private String storageZoneName;
    private String storagePassword; // Also serves as AccessKey for API
    private String pullZoneName;
    private String storageRegion; // e.g., "de", "uk", "ny", "la", "sg", "se", "br", "jh", "syd"
    private final Future<Void> configFuture;

    public BunnyImageProvider() {
        Console.log("[BUNNY] - Initializing BunnyImageProvider...");
        Promise<Void> configPromise = Promise.promise();
        configFuture = configPromise.future();
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> {
            if (config != null) {
                storageZoneName = config.getString("storageZoneName");
                storagePassword = config.getString("storagePassword");
                pullZoneName = config.getString("pullZoneName");
                storageRegion = config.getString("storageRegion");
                Console.log("[BUNNY] - Config loaded from " + CONFIG_PATH);
            }
            Console.log("[BUNNY] - Configuration:");
            Console.log("[BUNNY]   - Storage Zone: " + storageZoneName);
            Console.log("[BUNNY]   - Pull Zone: " + pullZoneName);
            Console.log("[BUNNY]   - Region: " + storageRegion);
            Console.log("[BUNNY]   - Storage Password: " + (storagePassword != null ? "***configured***" : "NOT SET"));
            configPromise.complete();
        });
    }

    @Override
    public Future<Void> readyFuture() {
        return configFuture;
    }

    // Public API (authentication not required)

    @Override
    public String urlPattern() {
        return "https://" + pullZoneName + ".b-cdn.net/:source?width=:width&height=:height";
    }

    @Override
    public String url(String source, int width, int height) {
        // Call parent's url() method which adds timestamp and handles URL building
        String url = super.url(idWithImageExtension(source), width, height);
        if (width < 0)
            url = url.replace("width=-1&", "");
        if (height < 0)
            url = url.replace("height=-1", "");
        return url;
    }

    @Override
    public Future<Boolean> exists(String id) {
        String url = getStorageApiEndpoint() + idWithImageExtension(id);
        return Fetch.fetch(
            url,
            new FetchOptions()
                .setMethod(HttpMethod.HEAD)
                .setHeaders(Fetch.createHeaders().set("AccessKey", storagePassword))
        ).map(response -> {
            boolean exists = response.ok();
            int status = response.status();
            if (status == 401) {
                Console.log("[BUNNY] - ⚠️ AUTHENTICATION FAILED (401)");
                Console.log("[BUNNY] - Check: 1) Storage zone name is correct: '" + storageZoneName + "'");
                Console.log("[BUNNY] - Check: 2) Storage password/AccessKey is correct in config");
                Console.log("[BUNNY] - Check: 3) Storage region is correct: '" + storageRegion + "'");
            }
            return exists;
        }).recover(error -> {
            Console.log("[BUNNY] - HEAD request error: " + error.getMessage());
            return Future.succeededFuture(false);
        });
    }

    /**
     * Ensures the path has an image extension. If not, .png is added by default
     */
    private String idWithImageExtension(String path) {
        // No change if an image extension is present
        if (path.matches(".*\\.(jpg|jpeg|gif|webp|svg|bmp|tiff)$")) {
            return path;
        }
        // Adding .png if no extension is present
        return path + ".png";
    }

    // Private API (authentication required)

    private String getStorageApiEndpoint() {
        // Map storage region to Storage API endpoint (NOT the CDN endpoint!)
        // Storage API is for upload/delete operations
        // CDN endpoint (b-cdn.net) is only for reading/serving images
        // Format: https://storage.bunnycdn.com/{storageZoneName}/ (default)
        //     or: https://{region}.storage.bunnycdn.com/{storageZoneName}/ (regional)
        String endpoint;
        if (storageRegion == null || storageRegion.isEmpty() || "de".equalsIgnoreCase(storageRegion)) {
            // Frankfurt (default region) - no region prefix
            endpoint = "storage.bunnycdn.com";
        } else {
            // Regional storage - add region prefix
            endpoint = storageRegion.toLowerCase() + ".storage.bunnycdn.com";
        }
        return "https://" + endpoint + "/" + storageZoneName + "/";
    }

    @Override
    public Future<Void> upload(Blob blob, String id, boolean overwrite) {
        // IMPORTANT: the blob format must match the image extension passed in `id` if present, or must be PNG if
        // no image extension is present in id.

        String idWithImageExtension = idWithImageExtension(id);
        String url = getStorageApiEndpoint() + idWithImageExtension;

        return Fetch.fetch(
            url,
            new FetchOptions()
                .setMethod(HttpMethod.PUT)
                .setHeaders(Fetch.createHeaders()
                    .set("AccessKey", storagePassword)
                    .set("Content-Type", "application/octet-stream"))
                .setBody(blob)
        ).compose(response -> {
            if (response.ok()) {
                return Future.succeededFuture();
            } else {
                return response.text().compose(errorText -> {
                    Console.log("[BUNNY] - Upload failed for '" + idWithImageExtension + "': " + errorText);
                    return Future.failedFuture("Upload failed: " + errorText);
                });
            }
        });
    }

    @Override
    public Future<Void> delete(String id, boolean invalidate) {
        String idWithImageExtension = idWithImageExtension(id);
        String url = getStorageApiEndpoint() + idWithImageExtension;

        return Fetch.fetch(
            url,
            new FetchOptions()
                .setMethod(HttpMethod.DELETE)
                .setHeaders(Fetch.createHeaders().set("AccessKey", storagePassword))
        ).compose(response -> {
            if (response.ok() || response.status() == 404) {
                // 200 = deleted successfully, 404 = already deleted
                return Future.succeededFuture();
            } else {
                return response.text().compose(errorText -> {
                    Console.log("[BUNNY] - Delete failed for '" + idWithImageExtension + "': " + errorText);
                    return Future.failedFuture("Delete failed: " + errorText);
                });
            }
        });
    }

}
