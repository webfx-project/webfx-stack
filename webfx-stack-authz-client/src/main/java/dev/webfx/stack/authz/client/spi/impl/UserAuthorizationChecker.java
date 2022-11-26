package dev.webfx.stack.authz.client.spi.impl;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface UserAuthorizationChecker {

    Future<Boolean> isAuthorized(Object operationAuthorizationRequest);

}
