package dev.webfx.stack.cloud.cloudinary;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.cloud.cloudinary.api.ApiResponse;

/**
 * @author Bruno Salmon
 */
public interface Search {

    Search expression(String value);

    Future<ApiResponse> execute();

}
