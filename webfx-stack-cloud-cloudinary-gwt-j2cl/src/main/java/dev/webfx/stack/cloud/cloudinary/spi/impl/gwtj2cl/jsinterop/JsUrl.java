package dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl.jsinterop;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * @author Bruno Salmon
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Url")
public interface JsUrl {

    JsUrl widthTransformation(double width);

    String generate(String source);

}
