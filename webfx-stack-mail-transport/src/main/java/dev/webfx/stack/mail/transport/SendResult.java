package dev.webfx.stack.mail.transport;

import java.time.Instant;

/**
 * Outcome of a successful hand-over to the transport. A failed transmission is reported
 * as a failed Future, not a SendResult.
 *
 * @author Bruno Salmon
 */
public final class SendResult {

    private final String messageId; // the Message-ID the mail left with (caller-supplied or provider-generated)
    private final boolean accepted;
    private final String provider;  // MailTransportProvider.getName() of the transport that sent it
    private final Instant at;

    public SendResult(String messageId, boolean accepted, String provider, Instant at) {
        this.messageId = messageId;
        this.accepted = accepted;
        this.provider = provider;
        this.at = at;
    }

    public String getMessageId() { return messageId; }
    public boolean isAccepted() { return accepted; }
    public String getProvider() { return provider; }
    public Instant getAt() { return at; }

    @Override
    public String toString() {
        return "SendResult{provider=" + provider + ", accepted=" + accepted + ", messageId=" + messageId + ", at=" + at + '}';
    }
}
