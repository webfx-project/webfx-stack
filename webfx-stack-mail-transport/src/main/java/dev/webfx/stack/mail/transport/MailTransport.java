package dev.webfx.stack.mail.transport;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.stack.mail.transport.spi.MailTransportProvider;

/**
 * Static facade over the configured {@link MailTransportProvider}. The provider is selected
 * and initialized asynchronously at boot (config-driven — see {@link MailTransportModuleBooter}),
 * so all access goes through a Future: callers composing on {@link #transmit} or
 * {@link #providerWhenReady} simply wait until the transport is ready instead of racing the boot.
 * If no provider is configured, the Future never completes and nothing is ever transmitted.
 *
 * @author Bruno Salmon
 */
public final class MailTransport {

    private static final Promise<MailTransportProvider> PROVIDER_PROMISE = Promise.promise();

    private MailTransport() {}

    // Called once by the booter after the configured provider initialized successfully.
    static void registerProvider(MailTransportProvider provider) {
        PROVIDER_PROMISE.complete(provider);
    }

    public static Future<MailTransportProvider> providerWhenReady() {
        return PROVIDER_PROMISE.future();
    }

    public static Future<SendResult> transmit(TransportMessage message) {
        return providerWhenReady().compose(provider -> provider.transmit(message));
    }

    /** The active provider's capabilities, or null while no provider is registered. */
    public static MailTransportCapabilities capabilities() {
        MailTransportProvider provider = PROVIDER_PROMISE.future().result();
        return provider == null ? null : provider.capabilities();
    }
}
