package dev.webfx.stack.authz.client.spi.impl.inmemory;

/**
 * @author Bruno Salmon
 */
public interface InMemoryAuthorizationRule {

    AuthorizationRuleResult computeRuleResult(Object authorizationRequest);

    Class<?> operationRequestClass(); // used for registration when coming from parsing

}
