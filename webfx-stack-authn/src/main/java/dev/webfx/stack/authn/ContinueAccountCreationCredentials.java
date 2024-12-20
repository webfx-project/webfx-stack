package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class ContinueAccountCreationCredentials {

    private final String token;

    public ContinueAccountCreationCredentials(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
