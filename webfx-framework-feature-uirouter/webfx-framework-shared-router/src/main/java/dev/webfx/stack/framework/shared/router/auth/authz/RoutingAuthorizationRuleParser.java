package dev.webfx.stack.framework.shared.router.auth.authz;

import dev.webfx.stack.framework.shared.services.authz.spi.impl.inmemory.AuthorizationRuleType;
import dev.webfx.stack.framework.shared.services.authz.spi.impl.inmemory.InMemoryAuthorizationRule;
import dev.webfx.stack.framework.shared.services.authz.spi.impl.inmemory.parser.SimpleInMemoryAuthorizationRuleParserBase;

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
