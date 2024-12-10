package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public class ContinueAccountCreationCredentials {

    private final String token;

    public ContinueAccountCreationCredentials(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
