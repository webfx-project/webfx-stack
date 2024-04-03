package dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl;

import dev.webfx.stack.cloud.cloudinary.api.ApiResponse;
import dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl.jsinterop.JsApiResponse;

/**
 * @author Bruno Salmon
 */
final class GwtJ2clApiResponse implements ApiResponse {

    private final JsApiResponse jsApiResponse;

    public GwtJ2clApiResponse(JsApiResponse jsApiResponse) {
        this.jsApiResponse = jsApiResponse;
    }

    @Override
    public Object get(String key) {
        return jsApiResponse.get(key);
    }

}
