package dev.webfx.stack.authz.core;

import dev.webfx.extras.operation.HasOperationCode;
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

    /** The line that declares an operation explicit-only: {@code explicit operation:<code>}. */
    public static final String EXPLICIT_OPERATION_PREFIX = "explicit operation:";

    /**
     * The operations a wildcard grant does not cover, declared by {@link #EXPLICIT_OPERATION_PREFIX} lines.
     *
     * <p>Held apart from the rules and from any context: being explicit-only is a property of the operation, not
     * of the section the line happens to sit in, so a declaration anywhere in the grant string applies everywhere.
     * Guarded by {@link #registeredInMemoryAuthorizationRules}, like the rules it filters.
     */
    private final Set<String> explicitOnlyOperationCodes = new HashSet<>();

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
            explicitOnlyOperationCodes.clear();
        }
    }

    /**
     * Declares an operation explicit-only: from now on a wildcard grant ({@code grant operation:*}) does not cover it,
     * and only a grant naming it does. A wildcard REVOKE still applies — refusing is never weakened.
     */
    public void registerExplicitOnlyOperation(String operationCode) {
        if (operationCode == null || operationCode.isEmpty())
            return;
        synchronized (registeredInMemoryAuthorizationRules) {
            explicitOnlyOperationCodes.add(operationCode);
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
        } else if (authorization.startsWith(EXPLICIT_OPERATION_PREFIX))
            registerExplicitOnlyOperation(authorization.substring(EXPLICIT_OPERATION_PREFIX.length()).trim());
        else
            registerAuthorizationRule(inMemoryAuthorizationRuleParser.parseAuthorization(authorization));
    }

    public boolean doesRulesAuthorize(Object operationRequest, Map<String, String> callerContext) {
        return computeRuleResult(operationRequest, callerContext) == AuthorizationRuleResult.GRANTED;
    }

    /**
     * Whether the rules authorize the request in SOME context the filter accepts — "do you hold this anywhere?",
     * for a request that has no target to take a context from.
     *
     * <p>True when the rules that declare no context grant it (they apply in every context), or when it is granted
     * in one of the registered contexts the filter accepts. Each such context is judged exactly as a caller standing
     * in it would be — with every rule that applies there, revokes and explicit-only declarations included — so this
     * never grants what {@link #doesRulesAuthorize} would refuse in every accepted context.
     *
     * <p>Only the contexts the rules themselves name are tried: a grant the caller would hold in a context nobody
     * registered is a grant from a context-free rule, which the first test already covers.
     *
     * @param contextFilter which registered contexts count — for "held for a whole organization", one keyed by the
     *                      organization alone (an event-scoped grant is not an answer about the organization)
     */
    public boolean doesRulesAuthorizeInSomeContext(Object operationRequest, java.util.function.Predicate<Map<String, String>> contextFilter) {
        if (doesRulesAuthorize(operationRequest, Collections.emptyMap()))
            return true;
        List<Map<String, String>> contexts;
        synchronized (registeredInMemoryAuthorizationRules) {
            contexts = new ArrayList<>(registeredInMemoryAuthorizationRules.keySet());
        }
        for (Map<String, String> context : contexts)
            if (!context.isEmpty() && contextFilter.test(Collections.unmodifiableMap(context))
                && doesRulesAuthorize(operationRequest, context))
                return true;
        return false;
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
            boolean explicitOnly = isExplicitOnly(operationRequest);
            // Every applicable context is evaluated on its own, and a refusal in ANY of them is final. Until 2026-11-17 the
            // contexts shared one result, and a context stopped as soon as that result was set - before reaching the
            // parsed rules, which are keyed under no request class (the end of the superclass walk). So whichever
            // context the map visited first decided alone: a context-free grant (a super administrator's wildcard)
            // hid a revoke in an organization section, and the other way round depending on the map's order.
            registeredInMemoryAuthorizationRules.forEach((context, operationRequestsRules) -> {
                if (result[0] == AuthorizationRuleResult.DENIED || !isContextApplicable(context, callerContext))
                    return;
                AuthorizationRuleResult contextResult = AuthorizationRuleResult.OUT_OF_RULE_CONTEXT;
                Class<?> operationRequestClass = operationRequest.getClass();
                while (true) {
                    Collection<InMemoryAuthorizationRule> rules = operationRequestsRules.get(operationRequestClass);
                    if (rules != null) {
                        synchronized (rules) { // Otherwise ConcurrentModificationException has been observed
                            for (InMemoryAuthorizationRule rule : rules) {
                                switch (rule.computeRuleResult(operationRequest)) {
                                    case DENIED:  result[0] = AuthorizationRuleResult.DENIED; return; // Final decision, for every context
                                    case GRANTED:
                                        // A wildcard does not grant an explicit-only operation: only a rule naming it does
                                        if (explicitOnly && rule.isWildcard())
                                            break;
                                        contextResult = AuthorizationRuleResult.GRANTED; // Not breaking, as we need to check if there is not another denying rule (denying rules have priority)
                                    case OUT_OF_RULE_CONTEXT: // just ignoring it and looping to the next
                                }
                            }
                        }
                    }
                    // The most specific request class with a rule decides for this context, as before
                    if (contextResult != AuthorizationRuleResult.OUT_OF_RULE_CONTEXT || operationRequestClass == null)
                        break;
                    operationRequestClass = operationRequestClass.getSuperclass();
                }
                if (contextResult == AuthorizationRuleResult.GRANTED)
                    result[0] = AuthorizationRuleResult.GRANTED; // Not DENIED here: that returned above
            });
        }
        return result[0];
    }

    /** Whether the request is for an operation declared explicit-only. Caller holds the rules' lock. */
    private boolean isExplicitOnly(Object operationRequest) {
        if (explicitOnlyOperationCodes.isEmpty() || !(operationRequest instanceof HasOperationCode))
            return false;
        Object operationCode = ((HasOperationCode) operationRequest).getOperationCode();
        return operationCode != null && explicitOnlyOperationCodes.contains(operationCode.toString());
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
