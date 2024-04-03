package dev.webfx.stack.cloud.cloudinary;

/**
 * @author Bruno Salmon
 */
public interface Url {

    Url widthTransformation(double width);

    String generate(String source);

}
