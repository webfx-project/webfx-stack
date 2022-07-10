package dev.webfx.stack.framework.shared.services.authz.spi;

import dev.webfx.stack.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthorizationServiceProvider {

    Future<Boolean> isAuthorized(Object operationAuthorizationRequest, Object userPrincipal);

}
