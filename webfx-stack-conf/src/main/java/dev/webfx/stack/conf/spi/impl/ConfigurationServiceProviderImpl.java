package dev.webfx.stack.conf.spi.impl;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.json.ReadOnlyJsonObject;
import dev.webfx.platform.util.keyobject.KeyObject;
import dev.webfx.platform.util.keyobject.ReadOnlyIndexedArray;
import dev.webfx.platform.util.keyobject.ReadOnlyKeyObject;
import dev.webfx.platform.util.serviceloader.MultipleServiceProviders;
import dev.webfx.stack.conf.ConfigurationService;
import dev.webfx.stack.conf.spi.ConfigurationServiceProvider;
import dev.webfx.stack.conf.spi.ConfigurationConsumer;
import dev.webfx.stack.conf.spi.ConfigurationFormat;
import dev.webfx.stack.conf.spi.ConfigurationSupplier;
import dev.webfx.stack.conf.spi.HasConfigurationLogInfo;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;

import static dev.webfx.stack.conf.ConfigurationService.VARIABLE_PATTERN;

/**
 * @author Bruno Salmon
 */
public class ConfigurationServiceProviderImpl implements ConfigurationServiceProvider {

    private final Map<String, ConfigurationFormat> formats = new HashMap<>();
    private final List<ConfigurationSupplier> suppliers = new ArrayList<>();
    private final Map<String, ConfigurationConsumer> consumers = new HashMap<>();

    // Note: this method is called by the ConfigurationModuleBooter, so all logs here will be displayed when booting
    // the application, at the configuration boot level.
    @Override
    public void boot() {
        // 1) Auto-registering the configuration formats declared as Java services
        registerProviders(">> Registered configuration formats:",
                MultipleServiceProviders.getProviders(ConfigurationFormat.class, () -> ServiceLoader.load(ConfigurationFormat.class)),
                this::registerConfigurationFormat,
                ConfigurationFormat::formatExtension);

        // TODO: improve sequencing knowing that suppliers and consumers boot may not be synchronous

        // 2) Auto-registering the configuration suppliers declared as Java services
        registerProviders(">> Registered configuration suppliers:",
                MultipleServiceProviders.getProviders(ConfigurationSupplier.class, () -> ServiceLoader.load(ConfigurationSupplier.class)),
                this::registerConfigurationSupplier);

        // 3) Auto-registering the consumers declared as Java services
        registerProviders(">> Booting registered configuration consumers:",
                MultipleServiceProviders.getProviders(ConfigurationConsumer.class, () -> ServiceLoader.load(ConfigurationConsumer.class)),
                this::registerConfigurationConsumer);
    }

    private <T> void registerProviders(String logHeader, List<T> providers, Consumer<T> registerMethod) {
        registerProviders(logHeader, providers, registerMethod, provider -> provider.getClass().getName());
    }

    private <T> void registerProviders(String logHeader, List<T> providers, Consumer<T> registerMethod, Function<T, String> nameGetter) {
        if (providers.isEmpty())
            Console.log(logHeader + " NONE");
        else {
            Console.log(logHeader);
            providers.forEach(provider -> {
                Console.log(" - " + nameGetter.apply(provider));
                registerMethod.accept(provider);
                if (provider instanceof HasConfigurationLogInfo)
                    Console.log(((HasConfigurationLogInfo) provider).getLogInfo());
            });
        }
    }

    @Override
    public void registerConfigurationFormat(ConfigurationFormat format) {
        formats.put(format.formatExtension().toLowerCase(), format);
    }

    @Override
    public void registerConfigurationSupplier(ConfigurationSupplier supplier) {
        suppliers.add(supplier);
        supplier.boot()
                .onFailure(e -> Console.log("❌ ERROR: " + e.getMessage()));
    }

    @Override
    public void registerConfigurationConsumer(ConfigurationConsumer consumer) {
        consumers.put(consumer.getConfigurationName(), consumer);
        consumer.boot()
                .onSuccess(ignored -> Console.log("✅ " + consumer.getConfigurationName()  + " configuration successfully resolved and passed to consumer"))
                .onFailure(e -> Console.log("❌ " + consumer.getConfigurationName() + " configuration is " + (e instanceof ConfigurationException && ((ConfigurationException) e).isPartial() ? "partially " : "") + "invalid"));
    }

    @Override
    public List<String> getRegisteredFormatExtensions() {
        return new ArrayList<>(formats.keySet());
    }

    @Override
    public ReadOnlyKeyObject readConfiguration(String configName, boolean resolveVariables) {
        // Getting the default configuration (if exists)
        ReadOnlyKeyObject defaultConfiguration = getDefaultConfiguration(configName);
        // Getting the configuration from the suppliers (if exists)
        ReadOnlyKeyObject supplierConfiguration = getSupplierConfiguration(configName, resolveVariables);
        // If no configuration could be found, we raise an exception
        if (defaultConfiguration == null && supplierConfiguration == null)
            return null; //throw new IllegalArgumentException("Configuration '" + configName + "' not found");
        // If there is no supplier configuration, we return the default configuration
        if (supplierConfiguration == null)
            return defaultConfiguration;
        if (defaultConfiguration == null)
            return supplierConfiguration;
        // If we have both the default and supplier configuration, we return the supplier configuration but completed
        // with the possible missing keys from the default configuration
        ReadOnlyIndexedArray keys = defaultConfiguration.keys();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getString(i);
            if (!supplierConfiguration.has(key))
                // Ugly hack for now (because don't know how to create a new KeyObject)
                ((KeyObject) supplierConfiguration).setScalar(key, defaultConfiguration.get(key));
        }
        return supplierConfiguration;
    }

    private ReadOnlyKeyObject getDefaultConfiguration(String configName) {
        ConfigurationConsumer consumer = consumers.get(configName);
        return consumer == null ? null : consumer.getDefaultConfiguration();
    }

    private ReadOnlyKeyObject getSupplierConfiguration(String configName, boolean resolveVariables) {
        for (ConfigurationSupplier supplier : suppliers)
            if (supplier.canReadConfiguration(configName))
                return supplier.readConfiguration(configName, resolveVariables);
        return null;
    }

    @Override
    public Future<Void> writeConfiguration(String configName, ReadOnlyKeyObject config) {
        // If there is a supplier that can write the requested configuration, we delegate that job to it.
        for (ConfigurationSupplier supplier : suppliers)
            if (supplier.canWriteConfiguration(configName))
                return supplier.writeConfiguration(configName, config);
        return Future.failedFuture("No supplier found to write configuration '" + configName + "'");
    }

    @Override
    public ReadOnlyKeyObject readConfigurationText(String configText, String formatExtension, boolean resolveVariables) {
        // Doing variables substitution at this point
        if (resolveVariables)
            configText = resolveVariables(configText);
        // Asking the configuration format to covert the result into a configuration object
        return getFormat(formatExtension).readConfigurationText(configText);
    }

    @Override
    public String writeConfigurationText(ReadOnlyJsonObject config, String formatExtension) {
        return getFormat(formatExtension).writeConfigurationText(config);
    }

    private ConfigurationFormat getFormat(String formatExtension) {
        formatExtension = ConfigurationService.getExtension(formatExtension);
        ConfigurationFormat format = formats.get(formatExtension);
        if (format == null)
            throw new RuntimeException("Unknown configuration format: " + formatExtension);
        return format;
    }

    private String resolveVariables(String configText) {
        Matcher matcher = VARIABLE_PATTERN.matcher(configText);
        StringBuilder sb = null;
        // Do we still have variable patterns?
        while (matcher.find()) {
            // If yes, we take the variable token, which may be a variable name (ex: SERVER_HOST) or an expression (ex: SERVER_HOST | 'localhost')
            String variableToken = matcher.group(1).trim();
            // We try to resolve that variable token (i.e. find the variable value, or evaluate the expression)
            Optional<String> variableValue = resolveVariableToken(variableToken);
            // If we can't, we don't do any replacement, but we log a warning reporting the variable couldn't be resolved
            if (variableValue.isEmpty())
                Console.log("⚠️ WARNING: Configuration variable " + variableToken + " couldn't be resolved");
            else {
                // The variable has been resolved. Note that the variable value may be another expression composed of
                // other variables, so we resolve it to cover this case.
                String replacement = resolveVariables(variableValue.get());

                // You can eventually uncomment the following log for debug purpose, but never keep it in production as
                // variable values can be secret (so not a good idea to log them).
                /* DEBUG ONLY: Console.log("Resolved configuration variable " + variableToken + " = " + replacement); */

                // We are almost ready for the replacement, we just ensure first that sb is not null
                if (sb == null)
                    sb = new StringBuilder();
                // Now we do the replacement, but we pass an empty string and not the replacement yet at this stage. The
                // reason is that matcher.appendReplacement(sb, replacement) can raise an exception if replacement
                // contains again the variable pattern ${{ XXX }}, which can happen if the resolution of the variable
                // value failed.
                matcher.appendReplacement(sb, ""); // append all text before the variable pattern, and then ""
                // Now we can append the replacement without raising an exception, even if replacement contains ${{ XXX }}
                sb.append(replacement);
            }
        }
        if (sb == null) // Happens when no variable pattern was found, or none of them could be resolved,
            return configText; // so we just return the original config text in this case.
        // In other cases (variable patterns were found and resolved), we append the possible remaining text
        return matcher.appendTail(sb).toString();
    }

    private Optional<String> resolveVariableToken(String variableToken) {
        Optional<String> o;
        int p1, p2 = -1;
        do {
            p1 = p2 + 1;
            p2 = variableToken.indexOf('|', p1);
            o = resolveVariable(variableToken.substring(p1, p2 == -1 ? variableToken.length() : p2).trim());
        } while (p2 != -1 && o.isEmpty());
        return o;
    }

    private Optional<String> resolveVariable(String variableName) {
        // true, false and numbers are constants, not variables, and don't need resolution
        if ("true".equals(variableName) || "false".equals(variableName) || Character.isDigit(variableName.charAt(0)))
            return Optional.of(variableName);
        // Same with literal strings such as 'localhost'
        if (variableName.startsWith("'") && variableName.endsWith("'"))
            return Optional.of(variableName.substring(1, variableName.length() - 1));
        // For all other cases, we try to resolve the variable by searching in the suppliers
        return suppliers.stream()
                .map(s -> s.resolveVariable(variableName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

}
