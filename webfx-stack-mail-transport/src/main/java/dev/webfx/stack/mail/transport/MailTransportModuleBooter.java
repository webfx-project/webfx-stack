package dev.webfx.stack.mail.transport;

import dev.webfx.platform.boot.spi.ApplicationModuleBooter;
import dev.webfx.platform.conf.Config;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.MultipleServiceProviders;
import dev.webfx.stack.mail.transport.spi.MailTransportProvider;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Selects the mail transport provider named by the {@code webfx.stack.mail.transport.provider}
 * config key among all providers present on the classpath, initializes it with its own config
 * subtree ({@code webfx.stack.mail.transport.<name>}), and registers it on the
 * {@link MailTransport} facade. With {@code provider = none} (the declared default) the module
 * is a no-op — nothing can be transmitted, which is the safe state for dev builds.
 *
 * @author Bruno Salmon
 */
public final class MailTransportModuleBooter implements ApplicationModuleBooter {

    private static final String MODULE_NAME = "webfx-stack-mail-transport";
    public static final String CONFIG_PATH = "webfx.stack.mail.transport";
    private static final String NO_PROVIDER = "none";

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    public int getBootLevel() {
        return COMMUNICATION_REGISTER_BOOT_LEVEL;
    }

    @Override
    public void bootModule() {
        // Defer to onConfigLoaded so ${{ VAR }} placeholders are resolved against the
        // deployment's variable files before we read the provider name.
        ConfigLoader.onConfigLoaded(CONFIG_PATH, this::onConfigLoaded);
    }

    private void onConfigLoaded(Config config) {
        String providerName = config == null ? null : config.getString("provider");
        if (providerName == null || providerName.isEmpty() || NO_PROVIDER.equals(providerName)) {
            Console.log("[" + MODULE_NAME + "] No mail transport provider configured — mail transmission disabled");
            return;
        }
        List<MailTransportProvider> providers = MultipleServiceProviders.getProviders(
                MailTransportProvider.class, () -> ServiceLoader.load(MailTransportProvider.class));
        MailTransportProvider provider = providers.stream()
                .filter(p -> providerName.equals(p.getName()))
                .findFirst()
                .orElse(null);
        if (provider == null) {
            Console.log("⛔️ [" + MODULE_NAME + "] No '" + providerName + "' mail transport provider found on the classpath (available: "
                    + providers.stream().map(MailTransportProvider::getName).collect(Collectors.joining(", ")) + ")");
            return;
        }
        provider.initialize(config.childConfigAt(providerName))
                .onFailure(e -> {
                    Console.log("⛔️ [" + MODULE_NAME + "] '" + providerName + "' mail transport provider failed to initialize");
                    Console.log(e);
                })
                .onSuccess(v -> {
                    MailTransport.registerProvider(provider);
                    Console.log("[" + MODULE_NAME + "] Mail transport ready: " + providerName);
                });
    }
}
