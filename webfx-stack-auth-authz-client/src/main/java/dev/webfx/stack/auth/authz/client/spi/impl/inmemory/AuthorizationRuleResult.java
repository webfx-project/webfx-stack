package dev.webfx.stack.auth.authz.client.spi.impl.inmemory;

/**
 * @author Bruno Salmon
 */
public enum AuthorizationRuleResult {
    GRANTED,
    DENIED,
    OUT_OF_RULE_CONTEXT
}
