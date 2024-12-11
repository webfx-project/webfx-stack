package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public class FinaliseAccountCreationCredentials {

    private final String token;
    private final String password;

    public FinaliseAccountCreationCredentials(String token, String password) {
        this.token = token;
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public String getPassword() {
        return password;
    }
}
