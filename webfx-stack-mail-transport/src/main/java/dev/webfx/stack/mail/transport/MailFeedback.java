package dev.webfx.stack.mail.transport;

import java.time.Instant;

/**
 * A delivery event reported by the transport (SES round): delivery confirmation, bounce
 * or complaint for a previously transmitted message.
 *
 * @author Bruno Salmon
 */
public final class MailFeedback {

    private final FeedbackType type;
    private final String email;
    private final String messageId;
    private final SuppressionReason reason;  // set for BOUNCE / COMPLAINT
    private final String diagnostic;         // bounce SMTP code / complaint detail when available
    private final Instant at;

    public MailFeedback(FeedbackType type, String email, String messageId, SuppressionReason reason, String diagnostic, Instant at) {
        this.type = type;
        this.email = email;
        this.messageId = messageId;
        this.reason = reason;
        this.diagnostic = diagnostic;
        this.at = at;
    }

    public FeedbackType getType() { return type; }
    public String getEmail() { return email; }
    public String getMessageId() { return messageId; }
    public SuppressionReason getReason() { return reason; }
    public String getDiagnostic() { return diagnostic; }
    public Instant getAt() { return at; }
}
