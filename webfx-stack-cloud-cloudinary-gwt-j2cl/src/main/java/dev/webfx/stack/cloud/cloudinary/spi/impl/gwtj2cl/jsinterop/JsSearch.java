package dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl.jsinterop;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.cloud.cloudinary.api.ApiResponse;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * @author Bruno Salmon
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Search")
public interface JsSearch {

    JsSearch expression(String value);

    Future<JsApiResponse> execute();

}
