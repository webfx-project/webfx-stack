package dev.webfx.stack.authz.client.spi.impl.inmemory;

import dev.webfx.stack.authz.client.spi.impl.inmemory.parser.InMemoryAuthorizationRuleParserRegistry;
import dev.webfx.stack.authz.client.spi.impl.inmemory.parser.InMemoryAuthorizationRuleParser;

import java.util.*;

/**
 * @author Bruno Salmon
 */
public final class InMemoryAuthorizationRuleRegistry implements InMemoryAuthorizationRule {

    private final Map<Class, Collection<InMemoryAuthorizationRule>> registeredInMemoryAuthorizationRules = new HashMap<>();
    private InMemoryAuthorizationRuleParser inMemoryAuthorizationRuleParser;

    public void setAuthorizationRuleParser(InMemoryAuthorizationRuleParser ruleParser) {
        this.inMemoryAuthorizationRuleParser = ruleParser;
    }

    public void addAuthorizationRuleParser(InMemoryAuthorizationRuleParser ruleParser) {
        if (inMemoryAuthorizationRuleParser == null)
            setAuthorizationRuleParser(ruleParser);
        else {
            InMemoryAuthorizationRuleParserRegistry registry;
            if (inMemoryAuthorizationRuleParser instanceof InMemoryAuthorizationRuleParserRegistry)
                registry = (InMemoryAuthorizationRuleParserRegistry) inMemoryAuthorizationRuleParser;
            else {
                registry = new InMemoryAuthorizationRuleParserRegistry();
                registry.registerParser(inMemoryAuthorizationRuleParser);
                setAuthorizationRuleParser(registry);
            }
            registry.registerParser(ruleParser);
        }
    }

    public void clearAllAuthorizationRules() {
        registeredInMemoryAuthorizationRules.clear();
    }

    public <A> void registerAuthorizationRule(Class<A> operationRequestClass, InMemoryAuthorizationRule<A> authorizationRule) {
        Collection<InMemoryAuthorizationRule> inMemoryAuthorizationRules = registeredInMemoryAuthorizationRules.get(operationRequestClass);
        if (inMemoryAuthorizationRules == null)
            registeredInMemoryAuthorizationRules.put(operationRequestClass, inMemoryAuthorizationRules = new ArrayList<>());
        inMemoryAuthorizationRules.add(authorizationRule);
    }

    public <A> void registerAuthorizationRule(InMemoryAuthorizationRule authorizationRule) {
        if (authorizationRule != null)
            registerAuthorizationRule(authorizationRule.operationRequestClass(), authorizationRule);
    }

    public void registerAuthorizationRule(String authorization) {
        registerAuthorizationRule(inMemoryAuthorizationRuleParser.parseAuthorization(authorization));
    }

    @Override
    public Class operationRequestClass() {
        return Object.class;
    }

    public boolean doesRulesAuthorize(Object operationAuthorizationRequest) {
        return computeRuleResult(operationAuthorizationRequest) == AuthorizationRuleResult.GRANTED;
    }

    @Override
    public AuthorizationRuleResult computeRuleResult(Object operationRequest) {
        AuthorizationRuleResult result = AuthorizationRuleResult.OUT_OF_RULE_CONTEXT;
        Class<?> operationRequestClass = operationRequest.getClass();
        while (true) {
            Collection<InMemoryAuthorizationRule> rules = registeredInMemoryAuthorizationRules.get(operationRequestClass);
            if (rules != null)
                for (InMemoryAuthorizationRule rule : rules)
                    switch (rule.computeRuleResult(operationRequest)) {
                        case DENIED:  result = AuthorizationRuleResult.DENIED; break; // Breaking as it's a final decision
                        case GRANTED: result = AuthorizationRuleResult.GRANTED; // Not breaking, as we need to check there is not another denying rule (which is priority)
                        case OUT_OF_RULE_CONTEXT: // just ignoring it and looping to the next
                    }
            if (result != AuthorizationRuleResult.OUT_OF_RULE_CONTEXT || operationRequestClass == null)
                break;
            operationRequestClass = operationRequestClass.getSuperclass();
        }
        return result;
    }

}
