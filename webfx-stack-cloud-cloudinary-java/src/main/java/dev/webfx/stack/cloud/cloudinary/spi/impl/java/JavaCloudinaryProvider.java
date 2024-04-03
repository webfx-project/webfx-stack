package dev.webfx.stack.cloud.cloudinary.spi.impl.java;

import dev.webfx.stack.cloud.cloudinary.Cloudinary;
import dev.webfx.stack.cloud.cloudinary.spi.CloudinaryProvider;

/**
 * @author Bruno Salmon
 */
public class JavaCloudinaryProvider implements CloudinaryProvider {

    @Override
    public Cloudinary createCloudinary(String cloudName, String apiKey, String apiSecret) {
        return new JavaCloudinary(cloudName, apiKey, apiSecret);
    }
}
