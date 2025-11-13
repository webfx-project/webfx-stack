package dev.webfx.stack.cloud.image.impl.bunny;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.blob.Blob;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.Headers;
import dev.webfx.platform.util.http.HttpHeaders;
import dev.webfx.platform.util.http.HttpMethod;
import dev.webfx.stack.cloud.image.spi.impl.fetch.FetchApiCloudImageProvider;

/**
 * @author Bruno Salmon
 */
public final class BunnyImageProvider extends FetchApiCloudImageProvider {

    private static final String CONFIG_PATH = "webfx.stack.cloud.image.bunny";

    private String storageZoneName;
    private String storagePassword; // Also serves as AccessKey for API
    private String storageRegion; // e.g., "de", "uk", "ny", "la", "sg", "se", "br", "jh", "syd"

    public BunnyImageProvider() {
        super(CONFIG_PATH);
    }

    @Override
    protected void onConfigLoaded(Config config) {
        storageZoneName = config.getString("storageZoneName");
        storagePassword = config.getString("storagePassword");
        String pullZoneName = config.getString("pullZoneName");
        storageRegion = config.getString("storageRegion");
        setUrlPattern("https://" + pullZoneName + ".b-cdn.net/:sourceWithImageExtension?(width=:width)&(height=:height)&t=:timestamp");
        Console.log("[BUNNY] - Configuration:");
        Console.log("[BUNNY]   - Storage Zone: " + storageZoneName);
        Console.log("[BUNNY]   - Pull Zone: " + pullZoneName);
        Console.log("[BUNNY]   - Region: " + storageRegion);
        Console.log("[BUNNY]   - Storage Password: " + (storagePassword != null ? "***configured***" : "NOT SET"));
    }

    @Override
    protected Headers createApiHeaders(String contentType) {
        return super.createApiHeaders(contentType)
            .set("AccessKey", storagePassword);
    }

    @Override
    public Future<Boolean> exists(String id) {
        String url = getStorageApiEndpoint() + sourceWithImageExtension(id);
        return fetchApi(url, HttpMethod.HEAD)
            .map(response -> {
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

    @Override
    public Future<Void> upload(Blob blob, String id, boolean overwrite) {
        // IMPORTANT: the blob format must match the image extension passed in `id` if present, or must be PNG if
        // no image extension is present in id.

        String idWithImageExtension = sourceWithImageExtension(id);
        String url = getStorageApiEndpoint() + idWithImageExtension;

        return fetchApi(url, HttpMethod.PUT, blob, HttpHeaders.APPLICATION_OCTET_STREAM)
            .compose(response -> {
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
        String idWithImageExtension = sourceWithImageExtension(id);
        String url = getStorageApiEndpoint() + idWithImageExtension;

        return fetchApi(url, HttpMethod.DELETE)
            .compose(response -> {
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

}
