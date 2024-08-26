package dev.webfx.stack.mail;

/**
 * @author Bruno Salmon
 */
public class MailMessageImpl implements MailMessage {

    private final String from;
    private final String to;
    private final String subject;
    private final String body;

    public MailMessageImpl(String from, String to, String subject, String body) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    @Override
    public String getFrom() {
        return from;
    }

    @Override
    public String getTo() {
        return to;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getBody() {
        return body;
    }
}
