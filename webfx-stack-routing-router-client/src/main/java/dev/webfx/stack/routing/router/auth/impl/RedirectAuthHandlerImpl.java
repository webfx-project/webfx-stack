package dev.webfx.stack.routing.router.auth.impl;

import dev.webfx.stack.authz.client.AuthorizationClientRequest;
import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.routing.router.auth.RedirectAuthHandler;
import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.router.spi.impl.client.ClientRoutingContextBase;
import dev.webfx.stack.session.state.client.fx.FXLoggedIn;
import dev.webfx.stack.session.state.client.fx.FXLoggedOut;

/**
 * @author Bruno Salmon
 */
public final class RedirectAuthHandlerImpl implements RedirectAuthHandler {

    private final String loginPath;
    private final String unauthorizedPath;
    private final Runnable onUnnecessaryLoginHandler;

    public RedirectAuthHandlerImpl(String loginPath, String unauthorizedPath, Runnable onUnnecessaryLoginHandler) {
        this.loginPath = loginPath;
        this.unauthorizedPath = unauthorizedPath;
        this.onUnnecessaryLoginHandler = onUnnecessaryLoginHandler;
    }

    @Override
    public void handle(RoutingContext context) {
        String requestedPath = context.path();
        boolean isLoginPath = requestedPath.equals(loginPath);
        boolean isUnauthorizedPath = requestedPath.equals(unauthorizedPath);
        boolean isRedirected = context instanceof ClientRoutingContextBase && ((ClientRoutingContextBase) context).isRedirected();
        if (isLoginPath && !isRedirected && onUnnecessaryLoginHandler != null && FXLoggedIn.isLoggedIn()) { // Should it be !LoggedOut.isLoggedOut()?
            onUnnecessaryLoginHandler.run();
        } else if (isLoginPath || isUnauthorizedPath) {
            context.next(); // Always accepting login and unauthorized paths
        } else { // Otherwise continuing the route only if the user is authorized, otherwise redirecting to auth page (login or unauthorized)
            new AuthorizationClientRequest<>()
                .setOperationRequest(new RouteRequest(requestedPath))
                .onAuthorizedExecute(context::next)
                .onUnauthorizedExecute(() -> redirectToAuth(context))
                .executeAsync();
        }
    }

    private void redirectToAuth(RoutingContext context) {
        RoutingContext authContext = ClientRoutingContextBase.newRedirectedContext(context, FXLoggedOut.isLoggedOut() ? loginPath : unauthorizedPath);
        authContext.next();
    }
}
