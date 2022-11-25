package dev.webfx.stack.authz.client.spi.impl.inmemory.parser;

import dev.webfx.stack.authz.client.spi.impl.inmemory.InMemoryAuthorizationRule;

/**
 * @author Bruno Salmon
 */
public interface InMemoryAuthorizationRuleParser {

    InMemoryAuthorizationRule parseAuthorization(String authorizationRule);

}
