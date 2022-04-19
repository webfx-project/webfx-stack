package dev.webfx.framework.shared.services.authz.spi;

import dev.webfx.platform.shared.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthorizationServiceProvider {

    Future<Boolean> isAuthorized(Object operationAuthorizationRequest, Object userPrincipal);

}
