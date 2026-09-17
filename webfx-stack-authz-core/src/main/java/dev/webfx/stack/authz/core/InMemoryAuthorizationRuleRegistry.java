package dev.webfx.stack.authz.core;

import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.authz.core.parser.InMemoryAuthorizationRuleParser;
import dev.webfx.stack.authz.core.parser.InMemoryAuthorizationRuleParserRegistry;

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

    public boolean doesRulesAuthorize(Object operationRequest, Map<String, String> callerContext) {
        return computeRuleResult(operationRequest, callerContext) == AuthorizationRuleResult.GRANTED;
    }

    /**
     * Evaluate the rules against a request, in the caller's context.
     *
     * <p>The context is a parameter rather than ambient state, and that is the change that let this
     * class leave the client. It used to read a static observable map describing what the UI was
     * looking at — one context per process, which is exactly right for one user driving one screen and
     * meaningless on a server handling many callers at once. Passing it in costs the client one
     * snapshot and lets the server answer per request.
     *
     * @param callerContext the context to judge rules against — for a UI, what it is showing; for a
     *                      server, what the request resolved to. Never null; pass an empty map when
     *                      there is no context, which matches only rules that declare none.
     */
    public AuthorizationRuleResult computeRuleResult(Object operationRequest, Map<String, String> callerContext) {
        AuthorizationRuleResult[] result ={ AuthorizationRuleResult.OUT_OF_RULE_CONTEXT };
        synchronized (registeredInMemoryAuthorizationRules) {
            registeredInMemoryAuthorizationRules.forEach((context, operationRequestsRules) -> {
                if (!isContextApplicable(context, callerContext))
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

    private boolean isContextApplicable(Map<String, String> ruleContext, Map<String, String> callerContext) {
        if (ruleContext != ANY_CONTEXT) {
            for (Map.Entry<String, String> contextProperty : ruleContext.entrySet()) {
                String value = contextProperty.getValue();
                if (!"any".equals(value)) {
                    String key = contextProperty.getKey();
                    if (!Objects.equals(value, callerContext == null ? null : callerContext.get(key)))
                        return false;
                }
            }
        }
        return true;
    }

}
