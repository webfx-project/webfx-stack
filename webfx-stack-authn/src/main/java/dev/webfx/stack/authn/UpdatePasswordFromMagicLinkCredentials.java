package dev.webfx.stack.authn;

/**
 * @author Bruno Salmon
 */
public record UpdatePasswordFromMagicLinkCredentials(String newPassword) {

    /** Redacted: a record's generated toString() would print the password into any message or log it reaches. */
    @Override
    public String toString() {
        return "UpdatePasswordFromMagicLinkCredentials[newPassword=***]";
    }
}
