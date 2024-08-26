package dev.webfx.stack.mail;

/**
 * @author Bruno Salmon
 */
public class MailMessageDelegate implements MailMessage {

    private final MailMessage delegate;

    public MailMessageDelegate(MailMessage delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getFrom() {
        return delegate.getFrom();
    }

    @Override
    public String getTo() {
        return delegate.getTo();
    }

    @Override
    public String getSubject() {
        return delegate.getSubject();
    }

    @Override
    public String getBody() {
        return delegate.getBody();
    }
}
