package dev.webfx.stack.cloud.cloudinary.spi.impl.java;

import com.cloudinary.utils.ObjectUtils;
import dev.webfx.stack.cloud.cloudinary.Cloudinary;
import dev.webfx.stack.cloud.cloudinary.Search;
import dev.webfx.stack.cloud.cloudinary.Uploader;
import dev.webfx.stack.cloud.cloudinary.Url;

/**
 * @author Bruno Salmon
 */
public final class JavaCloudinary implements Cloudinary {

    private com.cloudinary.Cloudinary jCloudinary;

    public JavaCloudinary(String cloudName, String apiKey, String apiSecret) {
        jCloudinary = new com.cloudinary.Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
    }

    @Override
    public Uploader uploader() {
        return new JavaUploader(jCloudinary.uploader());
    }

    @Override
    public Url url() {
        return new JavaUrl(jCloudinary.url());
    }

    @Override
    public Search search() {
        return new JavaSearch(jCloudinary.search());
    }
}
