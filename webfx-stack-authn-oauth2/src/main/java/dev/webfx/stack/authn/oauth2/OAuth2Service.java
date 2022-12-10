package dev.webfx.stack.authn.oauth2;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;
import dev.webfx.stack.authn.oauth2.spi.OAuth2Provider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class OAuth2Service {

    public static OAuth2Provider getProvider() {
        return SingleServiceProvider.getProvider(OAuth2Provider.class, () -> ServiceLoader.load(OAuth2Provider.class));
    }

    public static Future<Void> discover(String clientId, String clientSecret, String site) {
        return getProvider().discover(clientId, clientSecret, site);
    }
}
