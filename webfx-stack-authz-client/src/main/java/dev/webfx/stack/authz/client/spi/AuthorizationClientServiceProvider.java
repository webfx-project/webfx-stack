package dev.webfx.stack.authz.client.spi;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthorizationClientServiceProvider {

    Future<Boolean> isAuthorized(Object operationAuthorizationRequest, Object userPrincipal);

    Void onAuthorizationsPush(Object pushObject);

}
