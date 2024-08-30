package dev.webfx.stack.mail;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.mail.spi.MailServiceProvider;

import java.util.ServiceLoader;


/**
 * @author Bruno Salmon
 */
public class MailService {

    public static MailServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(MailServiceProvider.class, () -> ServiceLoader.load(MailServiceProvider.class));
    }

    public static Future<Void> sendMail(MailMessage mail) {
        return getProvider().sendMail(mail);
    }

}
