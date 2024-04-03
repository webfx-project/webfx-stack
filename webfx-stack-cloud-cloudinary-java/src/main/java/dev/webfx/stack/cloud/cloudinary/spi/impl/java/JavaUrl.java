package dev.webfx.stack.cloud.cloudinary.spi.impl.java;

import com.cloudinary.Transformation;
import dev.webfx.stack.cloud.cloudinary.Url;

/**
 * @author Bruno Salmon
 */
public class JavaUrl implements Url {

    private final com.cloudinary.Url jUrl;

    public JavaUrl(com.cloudinary.Url jUrl) {
        this.jUrl = jUrl;
    }

    @Override
    public Url widthTransformation(double width) {
        jUrl.transformation(new Transformation().width(width));
        return this;
    }

    @Override
    public String generate(String source) {
        return jUrl.generate(source);
    }
}
