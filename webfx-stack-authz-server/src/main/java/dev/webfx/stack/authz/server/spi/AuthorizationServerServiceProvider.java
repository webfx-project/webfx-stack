package dev.webfx.stack.authz.server.spi;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthorizationServerServiceProvider {

    Future<Void> pushAuthorizations();

}
