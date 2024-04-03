package dev.webfx.stack.cloud.cloudinary.spi.impl.java;

import dev.webfx.stack.cloud.cloudinary.api.ApiResponse;

import java.util.ArrayList;

/**
 * @author Bruno Salmon
 */
public class JavaApiResponse implements ApiResponse {

    private final com.cloudinary.api.ApiResponse jApiResponse;

    public JavaApiResponse(com.cloudinary.api.ApiResponse jApiResponse) {
        this.jApiResponse = jApiResponse;
    }

    @Override
    public Object get(String key) {
        return jApiResponse.get(key);
    }

}
