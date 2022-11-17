package dev.webfx.stack.auth.authz.spi.impl.inmemory;

/**
 * @author Bruno Salmon
 */
public enum AuthorizationRuleResult {
    GRANTED,
    DENIED,
    OUT_OF_RULE_CONTEXT
}
