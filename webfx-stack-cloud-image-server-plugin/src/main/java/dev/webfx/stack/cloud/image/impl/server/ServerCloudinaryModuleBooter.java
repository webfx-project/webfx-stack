package dev.webfx.stack.cloud.image.impl.server;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.file.spi.impl.jre.JreFile;
import dev.webfx.platform.util.Booleans;
import dev.webfx.platform.util.vertx.VertxInstance;
import dev.webfx.stack.cloud.image.CloudImageService;
import dev.webfx.stack.cloud.image.impl.cloudinary.Cloudinary;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static dev.webfx.platform.util.http.HttpResponseStatus.*;

/**
 * @author Bruno Salmon
 */
public class ServerCloudinaryModuleBooter implements ApplicationModuleBooter {

    private static final String CONFIG_PATH = "webfx.stack.cloud.image.rest";

    @Override
    public String getModuleName() {
        return "webfx-stack-cloud-image-server";
    }

    @Override
    public int getBootLevel() {
        return ApplicationModuleBooter.COMMUNICATION_REGISTER_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> {
            String existsPath = config.getString("existsPath");
            String uploadPath = config.getString("uploadPath");
            String deletePath = config.getString("deletePath");
            String urlPatternPath = config.getString("urlPatternPath");

            CloudImageService imageService = new Cloudinary(); // Temporarily hardcoded

            Router router = VertxInstance.getHttpRouter();

            // REST API for exists
            router.route(existsPath)
                .handler(BodyHandler.create())
                .handler(ctx -> {
                    HttpServerRequest request = ctx.request();
                    String id = request.getParam("id");
                    imageService.exists(id)
                            .onFailure(err -> ctx.response().setStatusCode(SERVICE_UNAVAILABLE_503).send())
                            .onSuccess(exists -> ctx.response().setStatusCode(exists ? OK_200 : NO_CONTENT_204).end());
                });

            // REST API for url pattern
            router.route(urlPatternPath)
                .handler(BodyHandler.create())
                .handler(ctx -> ctx.response().end(imageService.urlPattern()));

            // REST API for upload
            router.route(uploadPath)
                    .handler(BodyHandler.create())
                    .handler(ctx -> {
                        List<FileUpload> fileUploads = ctx.fileUploads();
                        if (fileUploads == null || fileUploads.size() != 1)
                            ctx.response().setStatusCode(BAD_REQUEST_400).end();
                        else {
                            FileUpload fileUpload = fileUploads.get(0);
                            File file = Path.of(fileUpload.uploadedFileName()).toFile();
                            JreFile javaFile = new JreFile(file, fileUpload.contentType());
                            String id = ctx.request().getParam("id");
                            boolean overwrite = Booleans.parseBoolean(ctx.request().getParam("overwrite"));
                            imageService.upload(javaFile, id, overwrite)
                                    .onComplete(ar -> {
                                        ctx.response().setStatusCode(ar.succeeded() ? OK_200 : NO_CONTENT_204).end();
                                        fileUpload.delete();
                                    });
                        }
                    });

            // REST API for delete
            router.route(deletePath)
                    .handler(BodyHandler.create())
                    .handler(ctx -> {
                        String id = ctx.request().getParam("id");
                        boolean invalidate = Booleans.parseBoolean(ctx.request().getParam("invalidate"));
                        imageService.delete(id, invalidate)
                                .onComplete(ar -> {
                                    ctx.response().setStatusCode(ar.succeeded() ? OK_200 : INTERNAL_SERVER_ERROR_500).end();
                                });
                    });

            });
    }

}
