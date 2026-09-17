package dev.webfx.stack.authz.server.spi;

import dev.webfx.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface AuthorizationServerServiceProvider {

    Future<Void> pushAuthorizations();

    /**
     * Whether the caller of the current request may perform what the request describes.
     *
     * <p>The counterpart of the client's check, and the reason this SPI exists at all. Until now it
     * could only PUSH a user's grants to the browser and trust the browser to act on them, which makes
     * the client's answer the only one — fine for hiding a button, worthless for refusing a write.
     *
     * <p>Implementations must read the principal from {@code ThreadLocalStateHolder} synchronously,
     * before their first async hop: the thread-local is restored when the synchronous portion of a call
     * returns, so anything continuing in a {@code compose()} sees no principal and would answer for
     * nobody.
     *
     * <p>Defaults to DENY rather than allow. An application that has not implemented this has not
     * decided anything, and a framework must not read silence as permission — a deployment that wires
     * enforcement without an implementation refuses everything, loudly, instead of admitting everyone,
     * quietly.
     */
    default Future<Boolean> isAuthorized(Object operationAuthorizationRequest) {
        return Future.succeededFuture(false);
    }

}
