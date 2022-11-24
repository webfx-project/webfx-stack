package dev.webfx.stack.auth.authz.server.spi;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthorizationServerServiceProvider {

    Future<Void> pushAuthorizations(Object userId, Object runId);

}
