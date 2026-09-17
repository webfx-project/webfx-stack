package dev.webfx.stack.authz.core.parser;

import dev.webfx.stack.authz.core.AuthorizationRuleType;
import dev.webfx.stack.authz.core.InMemoryAuthorizationRule;

/**
 * @author Bruno Salmon
 */
public abstract class SimpleInMemoryAuthorizationRuleParserBase implements InMemoryAuthorizationRuleParser {

    @Override
    public InMemoryAuthorizationRule parseAuthorization(String authorizationRule) {
        AuthorizationRuleType type = authorizationRule.startsWith("grant ") || authorizationRule.startsWith("GRANT ") ? AuthorizationRuleType.GRANT
                : authorizationRule.startsWith("revoke ") || authorizationRule.startsWith("REVOKE ") ? AuthorizationRuleType.REVOKE
                : null;
        return type == null ? null : parseAuthorization(type, authorizationRule.substring(6).trim());
    }

    protected abstract InMemoryAuthorizationRule parseAuthorization(AuthorizationRuleType type, String argument);
}
