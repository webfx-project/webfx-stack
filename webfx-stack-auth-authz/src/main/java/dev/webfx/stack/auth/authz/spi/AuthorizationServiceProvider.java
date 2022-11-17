package dev.webfx.stack.auth.authz.spi;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthorizationServiceProvider {

    Future<Boolean> isAuthorized(Object operationAuthorizationRequest, Object userPrincipal);

}
