package dev.webfx.stack.authn.server.gateway.spi.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.logout.server.LogoutPush;
import dev.webfx.stack.authn.server.gateway.spi.ServerAuthenticationGatewayProvider;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
/**
 * @author Bruno Salmon
 */
public abstract class ServerAuthenticationGatewayProviderBase implements ServerAuthenticationGatewayProvider {
    private final String gatewayAuthPrefix;

    public ServerAuthenticationGatewayProviderBase(String gatewayAuthPrefix) {
        this.gatewayAuthPrefix = gatewayAuthPrefix;
    }

    @Override
    public boolean acceptsUserCredentials(Object userCredentials) {
        return isStringStartingWithGatewayAuthPrefix(userCredentials);
    }

    protected boolean isStringStartingWithGatewayAuthPrefix(Object o) {
        return o instanceof String && ((String) o).startsWith(gatewayAuthPrefix);
    }

    protected String getAuthArgumentSuffix(Object argument) {
        return ((String) argument).substring(gatewayAuthPrefix.length());
    }

    @Override
    public Future<String> authenticate(Object userCredentials) {
        if (!isStringStartingWithGatewayAuthPrefix(userCredentials))
            return Future.failedFuture("Wrong argument for authenticate()");
        Promise<String> promise = Promise.promise();
        authenticateImpl(getAuthArgumentSuffix(userCredentials), promise);
        return promise.future();
    }

    protected abstract void authenticateImpl(String token, Promise<String> promise);

    @Override
    public boolean acceptsUserId() {
        Object userId = ThreadLocalStateHolder.getUserId();
        return isStringStartingWithGatewayAuthPrefix(userId);
    }

    @Override
    public Future<?> verifyAuthenticated() {
        Object userId = ThreadLocalStateHolder.getUserId();
        return getUserClaims().map(ignored -> userId);
    }

    @Override
    public Future<UserClaims> getUserClaims() {
        if (!acceptsUserId())
            return Future.failedFuture("Wrong argument for getUserClaims()");
        Promise<UserClaims> promise = Promise.promise();
        getUserClaimsImpl(getAuthArgumentSuffix(ThreadLocalStateHolder.getUserId()), promise);
        return promise.future();
    }

    protected abstract void getUserClaimsImpl(String token, Promise<UserClaims> promise);

    @Override
    public boolean acceptsUpdateCredentialsArgument(Object updateCredentialsArgument) {
        return false;
    }

    @Override
    public Future<?> updateCredentials(Object updateCredentialsArgument) {
        return Future.failedFuture("Unsupported operation: updateCredentials()");
    }

    @Override
    public Future<Void> logout() {
        return LogoutPush.pushLogoutMessageToClient();
    }

}
