package dev.webfx.stack.authn.server.gateway.spi.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.logout.server.LogoutPush;
import dev.webfx.stack.authn.server.gateway.spi.ServerAuthenticationGateway;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
/**
 * @author Bruno Salmon
 */
public abstract class ServerAuthenticationGatewayBase implements ServerAuthenticationGateway {
    private final String gatewayAuthPrefix;

    public ServerAuthenticationGatewayBase(String gatewayAuthPrefix) {
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

    /**
     * The same message, for a caller NO gateway recognises — which no instance can send, there being no
     * instance to send it.
     *
     * <p>Telling the client is the whole of what a gateway's logout does, as the method above shows. So a
     * caller whose identity has lapsed — the common case, not an exotic one: a token expired and refused,
     * reaching the portal as the logout sentinel — would otherwise be the one caller that logs out and is
     * never told, which is the same wedge as refusing them outright. See the portal's
     * {@code logoutThroughGateway}.
     *
     * <p>Static because there is nothing to call it on, and here rather than in the portal because this
     * module is the one that owns how a client is told.
     */
    public static Future<Void> pushLogoutMessageToUnrecognisedCaller() {
        return LogoutPush.pushLogoutMessageToClient();
    }

}
