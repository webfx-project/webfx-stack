package dev.webfx.stack.mail.transport.spi;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.conf.Config;
import dev.webfx.stack.mail.transport.MailFeedbackListener;
import dev.webfx.stack.mail.transport.MailTransportCapabilities;
import dev.webfx.stack.mail.transport.SendResult;
import dev.webfx.stack.mail.transport.SuppressionEntry;
import dev.webfx.stack.mail.transport.SuppressionQuery;
import dev.webfx.stack.mail.transport.TransportMessage;

import java.util.Collections;
import java.util.List;

/**
 * A mail transport: delivers a fully-addressed, fully-rendered email over the wire.
 * Implementations are discovered via ServiceLoader; the one whose {@link #getName()}
 * matches the {@code webfx.stack.mail.transport.provider} config key is initialized
 * and registered on the {@code MailTransport} facade at boot.
 *
 * <p>The feedback / suppression methods model delivery events (bounces, complaints)
 * for providers that support them (SES); send-only transports keep the inert defaults.
 *
 * @author Bruno Salmon
 */
public interface MailTransportProvider {

    /** The config name selecting this provider, e.g. "smtp", "sandbox", "aws-ses". */
    String getName();

    /**
     * Called once at boot with this provider's config subtree
     * (webfx.stack.mail.transport.&lt;name&gt;) before any transmit() call.
     */
    default Future<Void> initialize(Config providerConfig) {
        return Future.succeededFuture();
    }

    Future<SendResult> transmit(TransportMessage message);

    MailTransportCapabilities capabilities();

    // --- Delivery feedback / suppression (real implementations arrive with the SES provider) ---

    default void addFeedbackListener(MailFeedbackListener listener) {}

    default void removeFeedbackListener(MailFeedbackListener listener) {}

    default Future<List<SuppressionEntry>> listSuppressed(SuppressionQuery query) {
        return Future.succeededFuture(Collections.emptyList());
    }

    default Future<Void> removeSuppression(String email) {
        return Future.failedFuture("Suppression list not supported by the '" + getName() + "' mail transport");
    }
}
