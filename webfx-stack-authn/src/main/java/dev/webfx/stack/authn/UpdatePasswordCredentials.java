package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public record UpdatePasswordCredentials(String oldPassword, String newPassword) {

    /** Redacted: a record's generated toString() would print both passwords into any message or log it reaches. */
    @Override
    public String toString() {
        return "UpdatePasswordCredentials[oldPassword=***, newPassword=***]";
    }
}
