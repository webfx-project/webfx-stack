package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public final class AuthenticateWithUsernamePasswordCredentials {

    private final String username;
    private final String password;

    public AuthenticateWithUsernamePasswordCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
