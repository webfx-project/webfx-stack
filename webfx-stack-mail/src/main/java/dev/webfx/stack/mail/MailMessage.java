package dev.webfx.stack.mail;

/**
 * Simple draft version. To move closer to Vert.x model in future version.
 *
 * @author Bruno Salmon
 */
public interface MailMessage {

    String getFrom();

    String getTo();

    String getSubject();

    String getBody();

    static MailMessage create(String from, String to, String subject, String body) {
        return new MailMessageImpl(from, to, subject, body);
    }
}
