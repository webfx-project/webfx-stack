package dev.webfx.stack.mail.transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A fully-addressed, fully-rendered email ready for transmission. All routing decisions
 * (which From, which Reply-To, redirects…) are made by the caller BEFORE building this;
 * providers only transport it.
 *
 * <p>{@code headers} carries extra MIME headers (e.g. a caller-controlled Message-ID);
 * {@code tags} carries opaque provider hints (SMTP route, later SES configuration-set) —
 * see {@link TransportTags} for the well-known keys.
 *
 * @author Bruno Salmon
 */
public final class TransportMessage {

    private final MailAddress from;
    private final List<MailAddress> replyTo;
    private final List<MailAddress> to;
    private final List<MailAddress> cc;
    private final List<MailAddress> bcc;
    private final String subject;
    private final String textBody; // either or both of textBody / htmlBody
    private final String htmlBody;
    private final Map<String, String> headers;
    private final Map<String, String> tags;

    private TransportMessage(Builder builder) {
        this.from = Objects.requireNonNull(builder.from, "from");
        this.replyTo = Collections.unmodifiableList(new ArrayList<>(builder.replyTo));
        this.to = Collections.unmodifiableList(new ArrayList<>(builder.to));
        this.cc = Collections.unmodifiableList(new ArrayList<>(builder.cc));
        this.bcc = Collections.unmodifiableList(new ArrayList<>(builder.bcc));
        this.subject = builder.subject;
        this.textBody = builder.textBody;
        this.htmlBody = builder.htmlBody;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
        this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(builder.tags));
    }

    public MailAddress getFrom() { return from; }
    public List<MailAddress> getReplyTo() { return replyTo; }
    public List<MailAddress> getTo() { return to; }
    public List<MailAddress> getCc() { return cc; }
    public List<MailAddress> getBcc() { return bcc; }
    public String getSubject() { return subject; }
    public String getTextBody() { return textBody; }
    public String getHtmlBody() { return htmlBody; }
    public Map<String, String> getHeaders() { return headers; }
    public Map<String, String> getTags() { return tags; }

    public boolean hasRecipients() {
        return !to.isEmpty() || !cc.isEmpty() || !bcc.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private MailAddress from;
        private final List<MailAddress> replyTo = new ArrayList<>();
        private final List<MailAddress> to = new ArrayList<>();
        private final List<MailAddress> cc = new ArrayList<>();
        private final List<MailAddress> bcc = new ArrayList<>();
        private String subject;
        private String textBody;
        private String htmlBody;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final Map<String, String> tags = new LinkedHashMap<>();

        public Builder from(MailAddress from) { this.from = from; return this; }
        public Builder addReplyTo(MailAddress address) { replyTo.add(address); return this; }
        public Builder addTo(MailAddress address) { to.add(address); return this; }
        public Builder addCc(MailAddress address) { cc.add(address); return this; }
        public Builder addBcc(MailAddress address) { bcc.add(address); return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder textBody(String textBody) { this.textBody = textBody; return this; }
        public Builder htmlBody(String htmlBody) { this.htmlBody = htmlBody; return this; }
        public Builder header(String name, String value) { headers.put(name, value); return this; }
        public Builder tag(String key, String value) { tags.put(key, value); return this; }

        public TransportMessage build() {
            return new TransportMessage(this);
        }
    }
}
