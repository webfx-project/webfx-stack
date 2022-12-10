package dev.webfx.stack.authn.oauth2.spi;

import dev.webfx.platform.async.Future;

public interface OAuth2Provider {

    Future<Void> discover(String clientId, String clientSecret, String site);

}
