package dev.webfx.stack.routing.router.auth.authz;

import dev.webfx.stack.authz.client.spi.impl.inmemory.AuthorizationRuleType;
import dev.webfx.stack.authz.client.spi.impl.inmemory.SimpleInMemoryAuthorizationRuleBase;

/**
 * @author Bruno Salmon
 */
public final class RoutingAuthorizationRule extends SimpleInMemoryAuthorizationRuleBase<RouteRequest> {

    private final String route;
    private final boolean includeSubRoutes;

    public RoutingAuthorizationRule(AuthorizationRuleType type, String route, boolean includeSubRoutes) {
        super(type, RouteRequest.class);
        this.route = route;
        this.includeSubRoutes = includeSubRoutes;
    }

    @Override
    protected boolean matchRule(RouteRequest operationRequest) {
        String requestedRoute = operationRequest.getRoutePath();
        return requestedRoute != null && (requestedRoute.equals(route) || includeSubRoutes && requestedRoute.startsWith(route));
    }
}
