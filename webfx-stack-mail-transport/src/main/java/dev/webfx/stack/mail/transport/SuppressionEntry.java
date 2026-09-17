package dev.webfx.stack.mail.transport;

import java.time.Instant;

/**
 * An address the transport refuses to send to (bounced or complained), as reported by
 * providers with {@code capabilities().supportsSuppressionList()}.
 *
 * @author Bruno Salmon
 */
public final class SuppressionEntry {

    private final String email;
    private final SuppressionReason reason;
    private final Instant at;

    public SuppressionEntry(String email, SuppressionReason reason, Instant at) {
        this.email = email;
        this.reason = reason;
        this.at = at;
    }

    public String getEmail() { return email; }
    public SuppressionReason getReason() { return reason; }
    public Instant getAt() { return at; }
}
