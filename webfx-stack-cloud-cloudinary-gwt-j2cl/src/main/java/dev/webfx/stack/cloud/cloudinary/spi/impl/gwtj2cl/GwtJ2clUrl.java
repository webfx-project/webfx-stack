package dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl;

import dev.webfx.stack.cloud.cloudinary.Url;
import dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl.jsinterop.JsUrl;

/**
 * @author Bruno Salmon
 */
final class GwtJ2clUrl implements Url {

    private final JsUrl jsUrl;

    public GwtJ2clUrl(JsUrl jsUrl) {
        this.jsUrl = jsUrl;
    }

    @Override
    public Url widthTransformation(double width) {
        jsUrl.widthTransformation(width);
        return this;
    }

    @Override
    public String generate(String source) {
        return jsUrl.generate(source);
    }
}
