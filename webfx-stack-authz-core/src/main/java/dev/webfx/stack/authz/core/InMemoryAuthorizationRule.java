package dev.webfx.stack.authz.core;

/**
 * @author Bruno Salmon
 */
public interface InMemoryAuthorizationRule {

    AuthorizationRuleResult computeRuleResult(Object authorizationRequest);

    Class<?> operationRequestClass(); // used for registration when coming from parsing

}
