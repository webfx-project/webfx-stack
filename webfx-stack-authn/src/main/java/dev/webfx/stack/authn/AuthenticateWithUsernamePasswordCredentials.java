package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public record AuthenticateWithUsernamePasswordCredentials(String username, String password) {

    /** Redacted: a record's generated toString() would print the password into any message or log it reaches. */
    @Override
    public String toString() {
        return "AuthenticateWithUsernamePasswordCredentials[username=" + username + ", password=***]";
    }
}
