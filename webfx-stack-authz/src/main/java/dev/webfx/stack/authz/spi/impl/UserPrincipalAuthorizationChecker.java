package dev.webfx.stack.authz.spi.impl;

import dev.webfx.stack.async.Future;

/**
 * @author Bruno Salmon
 */
public interface UserPrincipalAuthorizationChecker {

    Object getUserPrincipal();

    Future<Boolean> isAuthorized(Object operationAuthorizationRequest);

}
