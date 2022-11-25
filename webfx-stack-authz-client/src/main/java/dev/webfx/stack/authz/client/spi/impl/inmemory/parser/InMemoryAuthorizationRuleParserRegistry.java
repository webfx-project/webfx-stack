package dev.webfx.stack.authz.client.spi.impl.inmemory.parser;

import dev.webfx.stack.authz.client.spi.impl.inmemory.InMemoryAuthorizationRule;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Bruno Salmon
 */
public final class InMemoryAuthorizationRuleParserRegistry implements InMemoryAuthorizationRuleParser {

    private final Collection<InMemoryAuthorizationRuleParser> parsers = new ArrayList<>();

    public void registerParser(InMemoryAuthorizationRuleParser parser) {
        parsers.add(parser);
    }

    public InMemoryAuthorizationRule parseAuthorization(String authorizationRule) {
        for (InMemoryAuthorizationRuleParser parser : parsers) {
            InMemoryAuthorizationRule parserdInMemoryAuthorizationRule = parser.parseAuthorization(authorizationRule);
            if (parserdInMemoryAuthorizationRule != null)
                return parserdInMemoryAuthorizationRule;
        }
        return null;
    }

}
