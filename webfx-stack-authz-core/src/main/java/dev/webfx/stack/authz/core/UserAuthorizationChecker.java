package dev.webfx.stack.authz.core;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface UserAuthorizationChecker {

    Future<Boolean> isAuthorized(Object operationAuthorizationRequest);

}
