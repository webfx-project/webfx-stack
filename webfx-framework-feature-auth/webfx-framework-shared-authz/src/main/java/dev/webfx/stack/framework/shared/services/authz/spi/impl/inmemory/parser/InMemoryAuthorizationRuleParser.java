package dev.webfx.stack.framework.shared.services.authz.spi.impl.inmemory.parser;

import dev.webfx.stack.framework.shared.services.authz.spi.impl.inmemory.InMemoryAuthorizationRule;

/**
 * @author Bruno Salmon
 */
public interface InMemoryAuthorizationRuleParser {

    InMemoryAuthorizationRule parseAuthorization(String authorizationRule);

}
