package dev.webfx.stack.routing.router.spi.impl.client;

import java.util.Collection;

/**
 * @author Bruno Salmon
 */
final class ClientClientRoutingContext extends ClientRoutingContextBase {

    private final ClientRouter router;
    private Throwable failure;
    private int statusCode = -1;

    ClientClientRoutingContext(String mountPoint, ClientRouter router, String path, Collection<ClientRoute> routes, Object state) {
        super(mountPoint, path, routes, state);
        this.router = router;
    }

    ClientRouter router() {
        return router;
    }

    @Override
    public Throwable failure() {
        return failure;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public boolean failed() {
        return failure != null || statusCode != -1;
    }

    @Override
    public void next() {
        if (!iterateNext())
            checkHandleNoMatch();
    }

    private void checkHandleNoMatch() {
        // Next called but no more matching routes
        if (failed())
            // Send back FAILURE
            unhandledFailure(statusCode, failure, router);
        else {
            unhandledFailure(404, new IllegalArgumentException("No matching route for path " + path), router);
            /*
            // Send back default 404
            response().setStatusCode(404);
            if (request().method() == HttpMethod.HEAD) {
                // HEAD responses don't have a body
                response().end();
            } else {
                response().end(DEFAULT_404);
            }*/
        }
    }

    //@Override
    public void fail(int statusCode) {
        this.statusCode = statusCode;
        doFail();
    }

    //@Override
    public void fail(Throwable t) {
        this.failure = t == null ? new NullPointerException() : t;
        doFail();
    }

    private void doFail() {
        this.iter = router.iterator();
        next();
    }

}
