package dev.webfx.stack.cloud.cloudinary.spi.impl.java;

import com.cloudinary.utils.ObjectUtils;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.file.File;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.stack.cloud.cloudinary.Uploader;

import java.io.IOException;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
final class JavaUploader implements Uploader {

    private final com.cloudinary.Uploader jUploader;

    public JavaUploader(com.cloudinary.Uploader jUploader) {
        this.jUploader = jUploader;
    }

    @Override
    public Future<Map> upload(File file, String publicId, boolean overwrite) {
        Promise<Map> promise = Promise.promise();
        Scheduler.runInBackground(() -> {
            try {
                Map result = jUploader.upload((java.io.File) file.getPlatformBlob(),
                        ObjectUtils.asMap("public_id", publicId, "overwrite", overwrite));
                promise.complete(result);
            } catch (IOException e) {
                promise.fail(e);
            }
        });
        return promise.future();
    }

    @Override
    public Future<Map> destroy(String publicId, boolean invalidate) {
        Promise<Map> promise = Promise.promise();
        Scheduler.runInBackground(() -> {
            try {
                Map result = jUploader.destroy(publicId, ObjectUtils.asMap("invalidate", invalidate));
                promise.complete(result);
            } catch (IOException e) {
                promise.fail(e);
            }
        });
        return promise.future();
    }
}
