package dev.webfx.stack.cloud.cloudinary;

import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.cloud.cloudinary.spi.CloudinaryProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class CloudinaryService {

    private CloudinaryService() {}

    private static CloudinaryProvider getProvider() {
        return SingleServiceProvider.getProvider(CloudinaryProvider.class, () -> ServiceLoader.load(CloudinaryProvider.class));
    }

    public static Cloudinary createCloudinary(String cloudName, String apiKey, String apiSecret) {
        return getProvider().createCloudinary(cloudName, apiKey, apiSecret);
    }

}
