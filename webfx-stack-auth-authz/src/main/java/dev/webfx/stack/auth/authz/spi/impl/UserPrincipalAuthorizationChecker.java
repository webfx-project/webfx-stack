package dev.webfx.stack.auth.authz.spi.impl;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface UserPrincipalAuthorizationChecker {

    Object getUserPrincipal();

    Future<Boolean> isAuthorized(Object operationAuthorizationRequest);

}
