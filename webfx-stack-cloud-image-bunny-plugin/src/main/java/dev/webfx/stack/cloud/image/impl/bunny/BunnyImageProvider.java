package dev.webfx.stack.cloud.image.impl.bunny;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.blob.Blob;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.*;
import dev.webfx.platform.util.http.HttpMethod;
import dev.webfx.stack.cloud.image.spi.impl.jsonfetchapi.JsonFetchApiCloudImageProvider;

/**
 * @author Bruno Salmon
 */
public final class BunnyImageProvider extends JsonFetchApiCloudImageProvider {

    private static final String CONFIG_PATH = "webfx.stack.cloud.image.bunny";

    private String storageZoneName;
    private String storagePassword; // Also serves as AccessKey for API
    private String pullZoneName;
    private String storageRegion; // e.g., "de", "uk", "ny", "la", "sg", "se", "br", "jh", "syd"
    private final Future<Void> configFuture;

    // Cache for discovered image paths (maps base path to full path with extension)
    private final java.util.Map<String, String> pathCache = new java.util.concurrent.ConcurrentHashMap<>();

    // All NEW images are stored as PNG - conversion happens before upload in application code
    // Legacy images may exist in other formats and are discovered via exists() checks

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
        return configFuture.onSuccess(v -> Console.log("[BUNNY] - Provider is ready and initialized"));
    }


    // Public API (authentication not required)

    public Future<Boolean> exists(String id) {
        // Try PNG first (new standard), then fallback to other formats for backward compatibility
        Console.log("[BUNNY] - Checking if image exists: " + id);
        return findExistingImage(id).map(foundPath -> foundPath != null);
    }

    /**
     * Finds an existing image by trying PNG first, then other common formats.
     * Returns the full path with extension if found, or null if not found.
     * Results are cached for performance.
     */
    private Future<String> findExistingImage(String id) {
        // Check cache first
        if (pathCache.containsKey(id)) {
            String cachedPath = pathCache.get(id);
            Console.log("[BUNNY] - Using cached path: " + id + " -> " + cachedPath);
            return Future.succeededFuture(cachedPath);
        }

        // If path already has an extension, check it directly
        if (id.matches(".*\\.(png|jpg|jpeg|gif|webp|svg|bmp|tiff)$")) {
            Console.log("[BUNNY] - Path already has extension: " + id);
            return checkFileExists(id).map(exists -> {
                if (exists) {
                    pathCache.put(id, id);
                    return id;
                }
                return null;
            });
        }

        // Try PNG first (new standard), then try all other common image formats
        String[] allFormats = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".svg", ".tiff", ".tif"};
        Console.log("[BUNNY] - Searching for file with any image extension...");

        return tryFormats(id, allFormats, 0).onSuccess(foundPath -> {
            if (foundPath != null) {
                pathCache.put(id, foundPath);
                Console.log("[BUNNY] - Cached found path: " + id + " -> " + foundPath);
            } else {
                Console.log("[BUNNY] - File not found after trying all extensions: " + id);
            }
        });
    }

    private Future<String> tryFormats(String id, String[] formats, int index) {
        if (index >= formats.length) {
            Console.log("[BUNNY] - Exhausted all " + formats.length + " format attempts for: " + id);
            return Future.succeededFuture(null);
        }

        String pathWithExt = id + formats[index];
        Console.log("[BUNNY] - [" + (index + 1) + "/" + formats.length + "] Trying: " + pathWithExt);

        return checkFileExists(pathWithExt).compose(exists -> {
            if (exists) {
                Console.log("[BUNNY] - ✓ FOUND: " + pathWithExt);
                return Future.succeededFuture(pathWithExt);
            } else {
                Console.log("[BUNNY] - ✗ Not found: " + pathWithExt);
                return tryFormats(id, formats, index + 1);
            }
        });
    }

    private Future<Boolean> checkFileExists(String pathWithExtension) {
        String url = getStorageApiEndpoint() + pathWithExtension;
        Console.log("[BUNNY] - HEAD request URL: " + url);
        Console.log("[BUNNY] - Using AccessKey: " + maskPassword(storagePassword));
        return Fetch.fetch(
                url,
                new FetchOptions()
                    .setMethod(HttpMethod.HEAD)
                    .setHeaders(Fetch.createHeaders().set("AccessKey", storagePassword))
        ).map(response -> {
            boolean exists = response.ok();
            int status = response.status();
            Console.log("[BUNNY] - HEAD response: " + status + " " + response.statusText() + " -> exists=" + exists);
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

    private String maskPassword(String password) {
        if (password == null || password.length() < 8) {
            return "***";
        }
        return password.substring(0, 4) + "..." + password.substring(password.length() - 4);
    }

    @Override
    public String urlPattern() {
        // Bunny.net uses query parameters for width/height instead of path segments
        String pattern = getRootUrl() + "/:source";
        Console.log("[BUNNY] - URL pattern: " + pattern);
        return pattern;
    }

    private String getRootUrl() {
        return "https://" + pullZoneName + ".b-cdn.net/";
    }

    @Override
    public String url(String source, int width, int height) {
        // Check cache for discovered path, otherwise default to PNG
        String sourceWithExtension;
        if (pathCache.containsKey(source)) {
            sourceWithExtension = pathCache.get(source);
            Console.log("[BUNNY] - Using cached path for URL: " + source + " -> " + sourceWithExtension);
        } else {
            // Default to PNG for new images (will be corrected when exists() is called)
            sourceWithExtension = ensurePngExtension(source);
            Console.log("[BUNNY] - No cache, defaulting to PNG: " + sourceWithExtension);
        }

        // Call parent's url() method which adds timestamp and handles URL building
        String url = super.url(sourceWithExtension, width, height);

        Console.log("[BUNNY] - Generated CDN URL for '" + source + "' (w:" + width + ", h:" + height + "): " + url);
        return url;
    }

    // Private API (authentication required)

    /**
     * Ensures the path has a .png extension.
     * All images are stored as PNG in Bunny storage.
     */
    private String ensurePngExtension(String path) {
        // Check if path already has .png extension
        if (path.endsWith(".png")) {
            return path;
        }
        // Remove any other image extension if present
        if (path.matches(".*\\.(jpg|jpeg|gif|webp|svg|bmp|tiff)$")) {
            path = path.replaceFirst("\\.(jpg|jpeg|gif|webp|svg|bmp|tiff)$", "");
        }
        return path + ".png";
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
        String storageUrl = "https://" + endpoint + "/" + storageZoneName + "/";
        Console.log("[BUNNY] - Storage API endpoint for region '" + storageRegion + "': " + storageUrl);
        return storageUrl;
    }

    public Future<Void> upload(Blob blob, String id, boolean overwrite) {
        // IMPORTANT: The blob must already be in PNG format before calling this method
        // Conversion from other formats (JPG, WebP, etc.) to PNG must happen in application code
        // before calling CloudImageService.upload()

        String idWithPng = ensurePngExtension(id);
        String url = getStorageApiEndpoint() + idWithPng;

        Console.log("[BUNNY] - Uploading image as PNG: " + id + " -> " + idWithPng);
        Console.log("[BUNNY] - Blob MIME type: " + blob.getMimeType());
        Console.log("[BUNNY] - Upload URL: " + url);
        Console.log("[BUNNY] - Blob size: " + blob.length() + " bytes");

        return Fetch.fetch(
                url,
                new FetchOptions()
                    .setMethod(HttpMethod.PUT)
                    .setHeaders(Fetch.createHeaders()
                        .set("AccessKey", storagePassword)
                        .set("Content-Type", "application/octet-stream"))
                    .setBody(blob)
        ).compose(response -> {
            Console.log("[BUNNY] - Upload response status: " + response.status() + " " + response.statusText());
            if (response.ok()) {
                Console.log("[BUNNY] - File uploaded successfully as PNG: " + idWithPng);
                // Cache the PNG path for future URL generation
                pathCache.put(id, idWithPng);
                Console.log("[BUNNY] - Cached PNG path: " + id + " -> " + idWithPng);
                return Future.succeededFuture();
            } else {
                return response.text().compose(errorText -> {
                    Console.log("[BUNNY] - Upload failed for '" + idWithPng + "': " + errorText);
                    return Future.failedFuture("Upload failed: " + errorText);
                });
            }
        });
    }

    public Future<Void> delete(String id, boolean invalidate) {
        // Find the actual file (might be PNG or legacy format)
        Console.log("[BUNNY] - ========================================");
        Console.log("[BUNNY] - DELETE REQUEST for: " + id);
        Console.log("[BUNNY] - ⚠️ CONFIGURATION CHECK:");
        Console.log("[BUNNY] -    Pull Zone (CDN): " + pullZoneName + ".b-cdn.net");
        Console.log("[BUNNY] -    Storage Zone: " + storageZoneName);
        Console.log("[BUNNY] -    Storage Region: " + storageRegion);
        Console.log("[BUNNY] - Storage endpoint: " + getStorageApiEndpoint());
        Console.log("[BUNNY] - ");
        Console.log("[BUNNY] - NOTE: If getting 401 errors, the Storage Zone name");
        Console.log("[BUNNY] -       or password is incorrect. Check Bunny.net dashboard:");
        Console.log("[BUNNY] -       1) Go to Storage > Click your zone > FTP & API Access");
        Console.log("[BUNNY] -       2) Verify Storage Zone Name matches: '" + storageZoneName + "'");
        Console.log("[BUNNY] -       3) Copy the correct Storage Password (AccessKey)");
        Console.log("[BUNNY] - ========================================");

        return findExistingImage(id).compose(foundPath -> {
            if (foundPath == null) {
                // File doesn't exist - consider deletion successful (idempotent)
                Console.log("[BUNNY] - ❌ File not found after checking all extensions");
                Console.log("[BUNNY] - Already deleted or never existed: " + id);
                pathCache.remove(id); // Clear from cache in case it was cached
                return Future.succeededFuture();
            }

            String url = getStorageApiEndpoint() + foundPath;
            Console.log("[BUNNY] - ✓ Found file to delete: " + foundPath);
            Console.log("[BUNNY] - Full DELETE URL: " + url);

            return Fetch.fetch(
                    url,
                    new FetchOptions()
                        .setMethod(HttpMethod.DELETE)
                        .setHeaders(Fetch.createHeaders().set("AccessKey", storagePassword))
            ).compose(response -> {
                Console.log("[BUNNY] - Delete response status: " + response.status() + " " + response.statusText());
                if (response.ok() || response.status() == 404) {
                    // 200 = deleted successfully, 404 = already deleted
                    Console.log("[BUNNY] - File deleted successfully: " + foundPath);
                    // Clear from cache
                    pathCache.remove(id);
                    Console.log("[BUNNY] - Removed from cache: " + id);
                    return Future.succeededFuture();
                } else {
                    return response.text().compose(errorText -> {
                        Console.log("[BUNNY] - Delete failed for '" + foundPath + "': " + errorText);
                        return Future.failedFuture("Delete failed: " + errorText);
                    });
                }
            });
        });
    }

}
