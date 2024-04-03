package dev.webfx.stack.cloud.cloudinary.spi.impl.gwtj2cl;

import dev.webfx.stack.cloud.cloudinary.Cloudinary;
import dev.webfx.stack.cloud.cloudinary.spi.CloudinaryProvider;

/**
 * @author Bruno Salmon
 */
public class GwtJ2clCloudinaryProvider implements CloudinaryProvider {

    @Override
    public Cloudinary createCloudinary(String cloudName, String apiKey, String apiSecret) {
        return new GwtJ2clCloudinary(cloudName, apiKey, apiSecret);
    }
}
