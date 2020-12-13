package dev.webfx.framework.shared.services.authz.spi.impl;

import dev.webfx.platform.shared.util.async.Future;

/**
 * @author Bruno Salmon
 */
public interface UserPrincipalAuthorizationChecker {

    Object getUserPrincipal();

    Future<Boolean> isAuthorized(Object operationAuthorizationRequest);

}
