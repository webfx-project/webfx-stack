package dev.webfx.stack.mail.transport.spi.impl.smtp.vertx;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.util.vertx.VertxInstance;
import dev.webfx.stack.mail.transport.MailAddress;
import dev.webfx.stack.mail.transport.MailTransportCapabilities;
import dev.webfx.stack.mail.transport.SendResult;
import dev.webfx.stack.mail.transport.TransportMessage;
import dev.webfx.stack.mail.transport.TransportTags;
import dev.webfx.stack.mail.transport.spi.MailTransportProvider;
import io.vertx.core.MultiMap;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SMTP transport on the Vert.x mail client. The route comes from the message's
 * {@link TransportTags} (falling back to the config defaults), so different messages can
 * travel through different relays — the Modality mailer resolves the route from each mail
 * account's SmtpAccount row, exactly like the legacy KBS2 sender did.
 *
 * <p>TLS mapping preserves the legacy JavaMail behaviour: ssl on port 587 → STARTTLS
 * required; ssl on any other port (typically 465) → TLS-on-connect.
 *
 * <p>User-supplied headers (Message-ID, Reply-To) REPLACE the encoder-generated ones —
 * the Vert.x MailEncoder applies {@code MailMessage.setHeaders} as the last step precisely
 * to allow a custom Message-ID.
 *
 * @author Bruno Salmon
 */
public final class SmtpVertxMailTransportProvider implements MailTransportProvider {

    private static final MailTransportCapabilities CAPABILITIES =
            new MailTransportCapabilities(false, false, false);

    private final Map<String, MailClient> mailClients = new ConcurrentHashMap<>();
    // Optional config fallbacks for deployments with a single fixed relay
    private String defaultHost;
    private Integer defaultPort;
    private Boolean defaultSsl;
    private String defaultUsername;
    private String defaultPassword;

    @Override
    public String getName() {
        return "smtp";
    }

    @Override
    public Future<Void> initialize(Config providerConfig) {
        if (providerConfig != null) {
            defaultHost = providerConfig.getString("defaultHost");
            defaultPort = providerConfig.getInteger("defaultPort");
            defaultSsl = providerConfig.getBoolean("defaultSsl");
            defaultUsername = providerConfig.getString("defaultUsername");
            defaultPassword = providerConfig.getString("defaultPassword");
        }
        return Future.succeededFuture();
    }

    @Override
    public MailTransportCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public Future<SendResult> transmit(TransportMessage message) {
        String host = tagOrDefault(message, TransportTags.SMTP_HOST, defaultHost);
        if (host == null || host.isEmpty())
            return Future.failedFuture("No SMTP host for this message (no '" + TransportTags.SMTP_HOST + "' tag and no defaultHost config)");
        String portTag = tagOrDefault(message, TransportTags.SMTP_PORT, defaultPort == null ? null : defaultPort.toString());
        int port = portTag == null ? 25 : Integer.parseInt(portTag);
        boolean ssl = Boolean.parseBoolean(tagOrDefault(message, TransportTags.SMTP_SSL, defaultSsl == null ? null : defaultSsl.toString()));
        String username = tagOrDefault(message, TransportTags.SMTP_USERNAME, defaultUsername);
        String password = tagOrDefault(message, TransportTags.SMTP_PASSWORD, defaultPassword);

        MailClient mailClient = mailClients.computeIfAbsent(host + ':' + port + ':' + username, key ->
                MailClient.createShared(VertxInstance.getVertx(), createMailConfig(host, port, ssl, username, password), key));

        Promise<SendResult> promise = Promise.promise();
        mailClient.sendMail(createMailMessage(message))
                .onSuccess(result -> promise.complete(new SendResult(result.getMessageID(), true, getName(), Instant.now())))
                .onFailure(promise::fail);
        return promise.future();
    }

    private String tagOrDefault(TransportMessage message, String tagKey, String defaultValue) {
        String value = message.getTags().get(tagKey);
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    private MailConfig createMailConfig(String host, int port, boolean ssl, String username, String password) {
        MailConfig mailConfig = new MailConfig()
                .setHostname(host)
                .setPort(port);
        if (ssl) {
            if (port == 587) // legacy JavaMail parity: 587 = submission port → STARTTLS, not TLS-on-connect
                mailConfig.setStarttls(StartTLSOptions.REQUIRED);
            else // typically 465
                mailConfig.setSsl(true);
        }
        if (username != null && !username.isEmpty()) {
            mailConfig.setLogin(LoginOption.REQUIRED)
                    .setUsername(username)
                    .setPassword(password);
        }
        return mailConfig;
    }

    private MailMessage createMailMessage(TransportMessage message) {
        MailMessage mailMessage = new MailMessage()
                .setFrom(toEncodedRfc822(message.getFrom()))
                .setSubject(message.getSubject());
        if (!message.getTo().isEmpty())
            mailMessage.setTo(toRfc822List(message.getTo()));
        if (!message.getCc().isEmpty())
            mailMessage.setCc(toRfc822List(message.getCc()));
        if (!message.getBcc().isEmpty())
            mailMessage.setBcc(toRfc822List(message.getBcc()));
        if (message.getHtmlBody() != null)
            mailMessage.setHtml(message.getHtmlBody());
        if (message.getTextBody() != null)
            mailMessage.setText(message.getTextBody());
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        // Reply-To has no first-class MailMessage field, so it travels as a raw header. Raw
        // headers bypass the encoder, so non-ASCII display names must be RFC 2047-encoded here.
        if (!message.getReplyTo().isEmpty())
            headers.set("Reply-To", message.getReplyTo().stream()
                    .map(SmtpVertxMailTransportProvider::toEncodedRfc822)
                    .collect(Collectors.joining(", ")));
        message.getHeaders().forEach(headers::set);
        if (!headers.isEmpty())
            mailMessage.setHeaders(headers);
        return mailMessage;
    }

    private static java.util.List<String> toRfc822List(java.util.List<MailAddress> addresses) {
        return addresses.stream().map(SmtpVertxMailTransportProvider::toEncodedRfc822).collect(Collectors.toList());
    }

    /**
     * RFC 2047 encoded-word for non-ASCII display names, plain RFC 5322 otherwise. Pre-encoding
     * here (rather than letting the Vert.x encoder handle it) keeps the address out of the
     * encoder's comment-form fallback, whose line folding can split a display name mid-word.
     */
    private static String toEncodedRfc822(MailAddress address) {
        String name = address.getName();
        if (name == null || name.isEmpty() || name.chars().allMatch(c -> c >= 32 && c < 127))
            return address.toRfc822();
        String encoded = Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + encoded + "?= <" + address.getEmail() + '>';
    }
}
