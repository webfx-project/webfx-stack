package dev.webfx.stack.mail.transport;

/**
 * @author Bruno Salmon
 */
@FunctionalInterface
public interface MailFeedbackListener {

    void onFeedback(MailFeedback feedback);

}
