package dev.webfx.stack.mail.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.mail.MailMessage;


/**
 * @author Bruno Salmon
 */
public interface MailServiceProvider {

    Future<Void> sendMail(MailMessage mailMessage);

}
