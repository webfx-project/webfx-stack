package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class FinaliseEmailUpdateCredentials {

    private final String token;

    public FinaliseEmailUpdateCredentials(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

}
