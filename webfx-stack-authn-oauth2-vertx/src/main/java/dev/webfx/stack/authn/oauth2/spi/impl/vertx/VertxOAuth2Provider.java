package dev.webfx.stack.authn.oauth2.spi.impl.vertx;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.util.vertx.VertxInstance;
import dev.webfx.stack.authn.oauth2.spi.OAuth2Provider;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth;

/**
 * @author Bruno Salmon
 */
public class VertxOAuth2Provider implements OAuth2Provider {

    @Override
    public Future<Void> discover(String clientId, String clientSecret, String site) {
        Promise<Void> promise = Promise.promise();
        OpenIDConnectAuth.discover(
                VertxInstance.getVertx(),
                new OAuth2Options()
                        .setClientId(clientId)
                        .setClientSecret(clientSecret)
                        .setSite(site)
        ).onComplete(ar -> {
            if (ar.failed())
                promise.fail(ar.cause());
            else
                promise.complete();
        });
        return promise.future();
    }

}
