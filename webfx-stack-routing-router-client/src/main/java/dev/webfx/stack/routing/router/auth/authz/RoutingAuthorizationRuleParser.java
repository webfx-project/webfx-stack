package dev.webfx.stack.routing.router.auth.authz;

import dev.webfx.stack.authz.core.AuthorizationRuleType;
import dev.webfx.stack.authz.core.InMemoryAuthorizationRule;
import dev.webfx.stack.authz.core.parser.SimpleInMemoryAuthorizationRuleParserBase;

/**
 * @author Bruno Salmon
 */
public final class RoutingAuthorizationRuleParser extends SimpleInMemoryAuthorizationRuleParserBase {

    @Override
    protected InMemoryAuthorizationRule parseAuthorization(AuthorizationRuleType type, String argument) {
        if (argument.startsWith("route:")) {
            String route = argument.substring(6).trim();
            boolean includeSubRoutes = route.endsWith("*");
            if (includeSubRoutes)
                route = route.substring(0, route.length() - 1);
            return new RoutingAuthorizationRule(type, route, includeSubRoutes);
        }
        return null;
    }

}
