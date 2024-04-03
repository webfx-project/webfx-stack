package dev.webfx.stack.cloud.cloudinary.spi;

import dev.webfx.stack.cloud.cloudinary.Cloudinary;

/**
 * @author Bruno Salmon
 */
public interface CloudinaryProvider {

    Cloudinary createCloudinary(String cloudName, String apiKey, String apiSecret);

}
