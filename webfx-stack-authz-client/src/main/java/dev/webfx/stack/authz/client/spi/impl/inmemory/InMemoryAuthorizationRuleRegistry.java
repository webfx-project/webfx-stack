package dev.webfx.stack.authz.client.spi.impl.inmemory;

import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.authz.client.context.AuthorizationContext;
import dev.webfx.stack.authz.client.spi.impl.inmemory.parser.InMemoryAuthorizationRuleParser;
import dev.webfx.stack.authz.client.spi.impl.inmemory.parser.InMemoryAuthorizationRuleParserRegistry;

import java.util.*;

/**
 * @author Bruno Salmon
 */
public final class InMemoryAuthorizationRuleRegistry {

    private final Map<String, String> ANY_CONTEXT = new HashMap<>();
    private Map<String, String> currentContext = ANY_CONTEXT;
    private final Map<Map<String, String> /* context */, Map<Class<?> /* operationRequestClass */, Collection<InMemoryAuthorizationRule>>> registeredInMemoryAuthorizationRules = new HashMap<>();
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
        synchronized (registeredInMemoryAuthorizationRules) {
            registeredInMemoryAuthorizationRules.clear();
        }
    }

    public <A> void registerAuthorizationRule(Class<?> operationRequestClass, InMemoryAuthorizationRule authorizationRule) {
        Collection<InMemoryAuthorizationRule> rules;
        // Ensure thread-safe creation/lookup of the list in the map
        synchronized (registeredInMemoryAuthorizationRules) {
            Map<Class<?>, Collection<InMemoryAuthorizationRule>> contextRules = registeredInMemoryAuthorizationRules.computeIfAbsent(currentContext, k -> new HashMap<>());
            rules = contextRules.computeIfAbsent(operationRequestClass, k -> new ArrayList<>());
        }
        // Protect list mutation while other threads may iterate
        synchronized (rules) {
            rules.add(authorizationRule);
        }
    }

    public void registerAuthorizationRule(InMemoryAuthorizationRule authorizationRule) {
        if (authorizationRule != null)
            registerAuthorizationRule(authorizationRule.operationRequestClass(), authorizationRule);
    }

    public void registerAuthorizationRule(String authorization) {
        if (authorization.equals("context:any"))
            currentContext = ANY_CONTEXT;
        else if (authorization.startsWith("context:")) {
            Map<String, String> contextProperties = new HashMap<>();
            String context = authorization.substring(8).trim();
            Arrays.forEach(context.split(","), contentProperty -> {
                String[] keyValue = contentProperty.trim().split("=");
                if (keyValue.length == 2)
                    contextProperties.put(keyValue[0], keyValue[1]);
            });
            currentContext = contextProperties;
        } else
            registerAuthorizationRule(inMemoryAuthorizationRuleParser.parseAuthorization(authorization));
    }

    public boolean doesRulesAuthorize(Object operationRequest) {
        return computeRuleResult(operationRequest) == AuthorizationRuleResult.GRANTED;
    }

    public AuthorizationRuleResult computeRuleResult(Object operationRequest) {
        AuthorizationRuleResult[] result ={ AuthorizationRuleResult.OUT_OF_RULE_CONTEXT };
        synchronized (registeredInMemoryAuthorizationRules) {
            registeredInMemoryAuthorizationRules.forEach((context, operationRequestsRules) -> {
                if (!isContextApplicable(context))
                    return;
                Class<?> operationRequestClass = operationRequest.getClass();
                while (true) {
                    Collection<InMemoryAuthorizationRule> rules = operationRequestsRules.get(operationRequestClass);
                    if (rules != null) {
                        synchronized (rules) { // Otherwise ConcurrentModificationException has been observed
                            for (InMemoryAuthorizationRule rule : rules) {
                                switch (rule.computeRuleResult(operationRequest)) {
                                    case DENIED:  result[0] = AuthorizationRuleResult.DENIED; return; // Breaking as it's a final decision
                                    case GRANTED: result[0] = AuthorizationRuleResult.GRANTED; // Not breaking, as we need to check if there is not another denying rule (denying rules have priority)
                                    case OUT_OF_RULE_CONTEXT: // just ignoring it and looping to the next
                                }
                            }
                        }
                    }
                    if (result[0] != AuthorizationRuleResult.OUT_OF_RULE_CONTEXT || operationRequestClass == null)
                        return;
                    operationRequestClass = operationRequestClass.getSuperclass();
                }
            });
        }
        return result[0];
    }

    private boolean isContextApplicable(Map<String, String> context) {
        if (context != ANY_CONTEXT) {
            for (Map.Entry<String, String> contextProperty : context.entrySet()) {
                String value = contextProperty.getValue();
                if (!"any".equals(value)) {
                    String key = contextProperty.getKey();
                    if (!Objects.equals(value, AuthorizationContext.getContextProperties().get(key)))
                        return false;
                }
            }
        }
        return true;
    }

}
