package dev.webfx.stack.mail.transport;

import java.util.Objects;

/**
 * An email address with an optional display name.
 *
 * @author Bruno Salmon
 */
public final class MailAddress {

    private final String email;
    private final String name; // optional display name, may be null

    public MailAddress(String email, String name) {
        this.email = Objects.requireNonNull(email, "email");
        this.name = name;
    }

    public static MailAddress of(String email) {
        return new MailAddress(email, null);
    }

    public static MailAddress of(String email, String name) {
        return new MailAddress(email, name);
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    /** RFC 5322 mailbox form: {@code Display Name <email>} or just {@code email} when unnamed. */
    public String toRfc822() {
        if (name == null || name.isEmpty())
            return email;
        // Quote the display name only when it contains RFC 5322 specials; plain names stay
        // unquoted so downstream encoders (which may re-encode non-ASCII names) see the bare text.
        String displayName = name;
        if (displayName.matches(".*[()<>\\[\\]:;@\\\\,.\"].*"))
            displayName = '"' + displayName.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        return displayName + " <" + email + '>';
    }

    @Override
    public String toString() {
        return toRfc822();
    }
}
