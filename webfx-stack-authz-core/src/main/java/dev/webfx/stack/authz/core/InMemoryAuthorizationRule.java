package dev.webfx.stack.authz.core;

/**
 * @author Bruno Salmon
 */
public interface InMemoryAuthorizationRule {

    AuthorizationRuleResult computeRuleResult(Object authorizationRequest);

    Class<?> operationRequestClass(); // used for registration when coming from parsing

    /**
     * Whether this rule covers operations without naming them (such as {@code grant operation:*}).
     *
     * <p>The registry asks it to keep a wildcard GRANT away from the operations declared explicit-only
     * ({@code explicit operation:<code>}), which only a rule naming them may grant. A rule that names
     * what it covers — an operation code, or a request class — is not a wildcard.
     */
    default boolean isWildcard() {
        return false;
    }

}
