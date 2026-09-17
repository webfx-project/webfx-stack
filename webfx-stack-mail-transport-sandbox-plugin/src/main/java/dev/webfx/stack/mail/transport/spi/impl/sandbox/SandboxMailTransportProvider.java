package dev.webfx.stack.mail.transport.spi.impl.sandbox;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.mail.transport.MailAddress;
import dev.webfx.stack.mail.transport.MailTransportCapabilities;
import dev.webfx.stack.mail.transport.SendResult;
import dev.webfx.stack.mail.transport.TransportMessage;
import dev.webfx.stack.mail.transport.spi.MailTransportProvider;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Swallow-and-log transport: every message "succeeds" without anything leaving the server.
 * The log line carries enough to follow the mail flow (addresses, subject, Message-ID);
 * bodies are logged only when the {@code logBody} config is set, as they may contain
 * personal data.
 *
 * @author Bruno Salmon
 */
public final class SandboxMailTransportProvider implements MailTransportProvider {

    private static final MailTransportCapabilities CAPABILITIES =
            new MailTransportCapabilities(false, false, false);

    private final AtomicLong sequence = new AtomicLong();
    private boolean logBody;

    @Override
    public String getName() {
        return "sandbox";
    }

    @Override
    public Future<Void> initialize(Config providerConfig) {
        logBody = providerConfig != null && Boolean.TRUE.equals(providerConfig.getBoolean("logBody"));
        return Future.succeededFuture();
    }

    @Override
    public MailTransportCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public Future<SendResult> transmit(TransportMessage message) {
        String messageId = message.getHeaders().get("Message-ID");
        if (messageId == null)
            messageId = "<sandbox-" + sequence.incrementAndGet() + "@localhost>";
        StringBuilder log = new StringBuilder("[mail-sandbox] Swallowed mail")
                .append("\n  From: ").append(message.getFrom());
        appendAddresses(log, "Reply-To", message.getReplyTo());
        appendAddresses(log, "To", message.getTo());
        appendAddresses(log, "Cc", message.getCc());
        appendAddresses(log, "Bcc", message.getBcc());
        log.append("\n  Subject: ").append(message.getSubject())
           .append("\n  Message-ID: ").append(messageId);
        if (logBody)
            log.append("\n  Body: ").append(message.getHtmlBody() != null ? message.getHtmlBody() : message.getTextBody());
        Console.log(log.toString());
        return Future.succeededFuture(new SendResult(messageId, true, getName(), Instant.now()));
    }

    private static void appendAddresses(StringBuilder log, String label, List<MailAddress> addresses) {
        if (!addresses.isEmpty())
            log.append("\n  ").append(label).append(": ")
               .append(addresses.stream().map(MailAddress::toString).collect(Collectors.joining(", ")));
    }
}
