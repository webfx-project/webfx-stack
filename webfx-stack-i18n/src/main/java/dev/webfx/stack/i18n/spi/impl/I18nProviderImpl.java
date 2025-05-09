package dev.webfx.stack.i18n.spi.impl;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.i18n.DefaultTokenKey;
import dev.webfx.stack.i18n.Dictionary;
import dev.webfx.stack.i18n.TokenKey;
import dev.webfx.stack.i18n.spi.I18nProvider;
import dev.webfx.stack.ui.fxraiser.FXRaiser;
import dev.webfx.stack.ui.fxraiser.FXValueRaiser;
import dev.webfx.stack.ui.fxraiser.impl.ValueConverterRegistry;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.stream.Collectors;

import static dev.webfx.platform.util.Objects.isAssignableFrom;

/**
 * @author Bruno Salmon
 */
public class I18nProviderImpl implements I18nProvider {

    private class TokenSnapshot {
        private final Dictionary dictionary;
        private final Object i18nKey;
        private final TokenKey tokenKey;
        private final Object tokenValue;
        private final I18nProviderImpl i18nProvider = I18nProviderImpl.this;

        public TokenSnapshot(Dictionary dictionary, Object i18nKey, TokenKey tokenKey, Object tokenValue) {
            this.dictionary = dictionary;
            this.i18nKey = i18nKey;
            this.tokenKey = tokenKey;
            this.tokenValue = tokenValue;
        }
    }

    static {
        ValueConverterRegistry.registerValueConverter(new FXValueRaiser() {
            @Override
            public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
                if (value instanceof TokenSnapshot) {
                    TokenSnapshot tokenSnapshot = (TokenSnapshot) value;
                    // Old code: value = tokenSnapshot.tokenValue; // this value may be deprecated (see explanation below).
                    // Although args are not handled here (they will be later), it's possible that the i18nKey internal
                    // state has changed. This happens, for example, in BookEventActivity (Modality front-office) with
                    // new I18nSubKey("expression: venue.address", FXEvent.eventProperty()), loadedProperty)
                    // where parentI18nKey = FX.eventProperty() is an entity that may not be completely loaded on the
                    // first i18n evaluation. The argument loadedProperty is actually not used in the evaluation itself;
                    // its purpose is just to trigger a new i18n evaluation once the entity is completely loaded. At
                    // this point, we need to refresh the value with a new i18n evaluation.
                    value = tokenSnapshot.i18nProvider.getFreshTokenValueFromSnapshot(tokenSnapshot);
                    if (value == null)
                        return null; // TODO: find a way to tell the ValueConverterRegistry that null is the actual final value
                    if (isAssignableFrom(raisedClass, value.getClass()))
                        return (T) value;
                }
                return null;
            }
        });
    }

    private final Map<Object/*i18nKey*/, Map<TokenKey, Reference<Property<TokenSnapshot>>>> liveDictionaryTokenProperties = new HashMap<>();
    private final Object defaultLanguage; // The language to find message parts (such as graphic) when missing in the current language
    private boolean dictionaryLoadRequired;
    private final DictionaryLoader dictionaryLoader;
    private Scheduled dictionaryLoadingScheduled;
    private final Set<Object> keysToLoad = new HashSet<>();
    private final Set<Object> defaultKeysToLoad = new HashSet<>();
    private final Set<Object> blacklistedKeys = new HashSet<>();
    private final FXValueRaiser i18nFxValueRaiser;

    public I18nProviderImpl(DictionaryLoader dictionaryLoader, Object defaultLanguage, Object initialLanguage) {
        this.dictionaryLoader = dictionaryLoader;
        if (defaultLanguage == null)
            defaultLanguage = guessDefaultLanguage();
        if (defaultLanguage == null) {
            if (initialLanguage != null)
                defaultLanguage = initialLanguage;
            else
                throw new IllegalArgumentException("No default/initial language set for I18n initialization");
        }
        this.defaultLanguage = defaultLanguage;
        if (initialLanguage == null)
            initialLanguage = guessInitialLanguage();
        if (initialLanguage == null)
            initialLanguage = defaultLanguage;
        setLanguage(initialLanguage);
        // We use FXRaiser to interpret arguments (see I18nProvider default methods), but we add here a final step
        // to interpret possible brackets AFTER arguments resolution. For example, i18n TimeFormat defines a key
        // called yearMonth2 whose value is [{1}] {0} (in English), which after arguments resolution can be [february] 25
        // and [february] still needs to be interpreted by i8n. That's what we are doing here.
        i18nFxValueRaiser = new FXValueRaiser() {
            @Override
            public <T> T raiseValue(Object value, Class<T> raisedClass, Object... args) {
                // Doing default arguments resolution
                T raisedValue = FXRaiser.getFxValueRaiserInstance().raiseValue(value, raisedClass, args);
                // Doing post-bracket interpretation (works only for TEXT token)
                if (raisedValue instanceof String && ((String) raisedValue).contains("[")) {
                    Dictionary dictionary = getDictionary();
                    raisedValue = (T) interpretBracketsAndDefaultInTokenValue(raisedValue, null, "", DefaultTokenKey.TEXT, dictionary, false, getDefaultDictionary(), true);
                }
                return raisedValue;
            }
        };
    }

    @Override
    public FXValueRaiser getI18nFxValueRaiser() {
        return i18nFxValueRaiser;
    }

    private Object guessDefaultLanguage() {
        return getSupportedLanguages().stream().findFirst().orElse(null);
    }

    private Object guessInitialLanguage() {
        return null;
    }

    private final ObjectProperty<Object> languageProperty = FXProperties.newObjectProperty(this::onLanguageChanged);

    @Override
    public ObjectProperty<Object> languageProperty() {
        return languageProperty;
    }

    private final ObjectProperty<Dictionary> dictionaryProperty = new SimpleObjectProperty<>();

    @Override
    public ObservableObjectValue<Dictionary> dictionaryProperty() {
        return dictionaryProperty;
    }

    @Override
    public Object getDefaultLanguage() {
        return defaultLanguage;
    }

    private final Property<Dictionary> defaultDictionaryProperty = new SimpleObjectProperty<>();

    @Override
    public Dictionary getDefaultDictionary() {
        return defaultDictionaryProperty.getValue();
    }

    /// NEW API

    @Override
    public <TK extends Enum<?> & TokenKey> Object getDictionaryTokenValue(Object i18nKey, TK tokenKey, Dictionary dictionary) {
        if (dictionary == null)
            dictionary = getDictionary();
        return getDictionaryTokenValueImpl(i18nKey, tokenKey, dictionary, false, dictionary, false, false);
    }

    protected <TK extends Enum<?> & TokenKey> Object getDictionaryTokenValueImpl(Object i18nKey, TK tokenKey, Dictionary dictionary, boolean skipDefaultDictionary, Dictionary originalDictionary, boolean skipMessageKeyInterpretation, boolean skipMessageLoading) {
        Object tokenValue = null;
        String tokenValuePrefix = null, tokenValueSuffix = null;
        if (i18nKey != null) {
            Object messageKey = i18nKeyToDictionaryMessageKey(i18nKey);
            tokenValue = dictionary == null ? null : dictionary.getMessageTokenValue(messageKey, tokenKey, false);
            // Message key prefix and suffix interpretation
            if (tokenValue == null && !skipMessageKeyInterpretation && messageKey instanceof String) {
                String sKey = (String) messageKey;
                int length = Strings.length(sKey);
                // Prefix interpretation (only << for now)
                if (length > 1) {
                    int index = 0;
                    while (index < length && !Character.isLetterOrDigit(sKey.charAt(index)))
                        index++;
                    if (index > 0) {
                        String sKeyPrefix = sKey.substring(0, index);
                        switch (sKeyPrefix) {
                            case "<<":
                                // Reading the token value of the remaining key (after <<)
                                tokenValue = getDictionaryTokenValueImpl(new I18nSubKey(sKey.substring(sKeyPrefix.length(), length), i18nKey), tokenKey, dictionary, skipDefaultDictionary, originalDictionary, true, skipMessageLoading);
                                if (tokenValue != null && isAssignableFrom(tokenKey.expectedClass(), String.class))
                                    tokenValuePrefix = "" + getDictionaryTokenValueImpl(sKeyPrefix, tokenKey, dictionary, skipDefaultDictionary, originalDictionary, true, skipMessageLoading);
                        }
                    }
                }
                // Suffix interpretation (:, ?, >>, ...)
                if (tokenValue == null && length > 1) {
                    int index = length;
                    while (index > 0 && !Character.isLetterOrDigit(sKey.charAt(index - 1)))
                        index--;
                    if (index < length) {
                        String sKeySuffix = sKey.substring(index, length);
                        switch (sKeySuffix) {
                            case ":":
                            case "?":
                            case ">>":
                            case "...":
                                // Reading the token value of the remaining key (before the suffix)
                                tokenValue = getDictionaryTokenValueImpl(new I18nSubKey(sKey.substring(0, length - sKeySuffix.length()), i18nKey), tokenKey, dictionary, skipDefaultDictionary, originalDictionary, true, skipMessageLoading);
                                if (tokenValue != null && isAssignableFrom(tokenKey.expectedClass(), String.class))
                                    tokenValueSuffix = "" + getDictionaryTokenValueImpl(sKeySuffix, tokenKey, dictionary, skipDefaultDictionary, originalDictionary, true, skipMessageLoading);
                        }
                    }
                }
                // Case transformer keys
                if (tokenValue == null && length > 1 && dictionary != null) {
                    // Second search but ignoring the case
                    tokenValue = dictionary.getMessageTokenValue(messageKey, tokenKey, true);
                    if (tokenValue != null) { // Yes, we found a value this time!
                        String sValue = Strings.toString(tokenValue);
                        if (sKey.equals(sKey.toUpperCase())) // was key in uppercase? => we uppercase the value
                            tokenValue = sValue.toUpperCase();
                        else if (sKey.equals(sKey.toLowerCase())) // was key in lowercase? => we lowercase the value
                            tokenValue = sValue.toLowerCase();
                        else if (!sValue.isEmpty()) {
                            char firstCharKey = sKey.charAt(0);
                            char firstCharValue = sValue.charAt(0);
                            if (Character.isUpperCase(firstCharKey)) { // was the first letter in uppercase? => we uppercase the first letter in value
                                if (!Character.isUpperCase(firstCharValue))
                                    tokenValue = Character.toUpperCase(firstCharValue) + sValue.substring(1);
                            } else { // was the first letter lowercase? => we lowercase the first letter in value
                                if (!Character.isLowerCase(firstCharValue))
                                    tokenValue = Character.toLowerCase(firstCharValue) + sValue.substring(1);
                            }
                        }
                    }
                }
            }
            tokenValue = interpretBracketsAndDefaultInTokenValue(tokenValue, messageKey, i18nKey, tokenKey, dictionary, skipDefaultDictionary, originalDictionary, skipMessageLoading);
        }
        // Temporary code which is a workaround for the YAML parser not able to parse line feeds in strings.
        if (tokenValue instanceof String) // TODO: remove this workaround once yaml parser is fixed
            tokenValue = ((String) tokenValue).replace("\\n", "\n");
        if (tokenValuePrefix != null)
            tokenValue = tokenValuePrefix + tokenValue;
        if (tokenValueSuffix != null)
            tokenValue = tokenValue + tokenValueSuffix;
        return tokenValue;
    }

    // public because called by AstDictionary to interpret token values within Ast objects as well
    public <TK extends Enum<?> & TokenKey> Object interpretBracketsAndDefaultInTokenValue(Object tokenValue, Object messageKey, Object i18nKey, TK tokenKey, Dictionary dictionary, boolean skipDefaultDictionary, Dictionary originalDictionary, boolean skipMessageLoading) {
        // Token value bracket interpretation: if the value contains an i18n key in bracket, we interpret it
        if (tokenValue instanceof String || tokenValue == null && messageKey instanceof String) {
            String sToken = (String) (tokenValue == null ? messageKey : tokenValue);
            int i1 = sToken.indexOf('[');
            if (i1 >= 0) {
                int i2 = i1 == 0 && sToken.endsWith("]") ? sToken.length() - 1 : sToken.indexOf(']', i1 + 1);
                if (i2 > 0) {
                    // Note: we always use originalDictionary for the resolution, because even if that token value
                    // comes from the default dictionary (ex: EN), we still want the brackets to be interpreted in
                    // the original language (ex: FR).
                    String bracketToken = sToken.substring(i1 + 1, i2);
                    // Note: brackets such as [{0}] will be interpreted later by i18nFxValueRaiser, so we skip them here
                    if (!(bracketToken.startsWith("{") && bracketToken.endsWith("}"))) {
                        Object resolvedValue = getDictionaryTokenValueImpl(new I18nSubKey(bracketToken, i18nKey), tokenKey, dictionary, false, originalDictionary, false, skipMessageLoading);
                        // If the bracket token has been resolved, we return it with the parts before and after the brackets
                        if (resolvedValue != null) {
                            if (i1 == 0 && i2 == sToken.length() - 1) // except if there are no parts before and after the brackets
                                tokenValue = resolvedValue; // in which case we return the resolved object as is (possibly not a String)
                            else
                                tokenValue = (i1 == 0 ? "" : sToken.substring(0, i1)) + resolvedValue + sToken.substring(i2 + 1);
                        }
                    }
                }
            }
        }
        if (tokenValue == null && !skipDefaultDictionary) {
            Dictionary defaultDictionary = getDefaultDictionary();
            if (dictionary != defaultDictionary && defaultDictionary != null)
                tokenValue = getDictionaryTokenValueImpl(i18nKey, tokenKey, defaultDictionary, true, originalDictionary, false, skipMessageLoading); //getI18nPartValue(tokenSnapshot.i18nKey, part, defaultDictionary, skipPrefixOrSuffix);
            if (tokenValue == null) {
                if (!skipMessageLoading)
                    scheduleMessageLoading(i18nKey, true);
                if (tokenKey == DefaultTokenKey.TEXT || tokenKey == DefaultTokenKey.GRAPHIC) // we use it also for graphic in Modality after evaluating an expression that gives the path to the icon
                    tokenValue = messageKey; //;whatToReturnWhenI18nTextIsNotFound(tokenSnapshot.i18nKey, tokenSnapshot.tokenKey);
            }
        }
        return tokenValue;
    }

    @Override
    public <TK extends Enum<?> & TokenKey> ObservableValue<?> dictionaryTokenProperty(Object i18nKey, TK tokenKey, Object... args) {
        Property<TokenSnapshot> dictionaryTokenProperty = getLiveDictionaryTokenProperty(i18nKey, tokenKey);
        if (dictionaryTokenProperty == null)
            getLiveMessageMap(i18nKey, true).put(tokenKey, new SoftReference<>(dictionaryTokenProperty = createLiveDictionaryTokenProperty(i18nKey, tokenKey)));
        return dictionaryTokenProperty;
    }

    private <TK extends Enum<?> & TokenKey> Property<TokenSnapshot> getLiveDictionaryTokenProperty(Object i18nKey, TK tokenKey) {
        Map<TokenKey, Reference<Property<TokenSnapshot>>> messageMap = getLiveMessageMap(i18nKey, false);
        if (messageMap == null)
            return null;
        Reference<Property<TokenSnapshot>> ref = messageMap.get(tokenKey);
        return ref == null ? null : ref.get();
    }

    private Map<TokenKey, Reference<Property<TokenSnapshot>>> getLiveMessageMap(Object i18nKey, boolean createIfNotExists) {
        Map<TokenKey, Reference<Property<TokenSnapshot>>> messageMap = liveDictionaryTokenProperties.get(i18nKey);
        if (messageMap == null && createIfNotExists)
            synchronized (liveDictionaryTokenProperties) {
                liveDictionaryTokenProperties.put(i18nKey, messageMap = new HashMap<>());
            }
        return messageMap;
    }

    private <TK extends Enum<?> & TokenKey> Property<TokenSnapshot> createLiveDictionaryTokenProperty(Object i18nKey, TK tokenKey) {
        return refreshDictionaryTokenSnapshot(new SimpleObjectProperty<>(new TokenSnapshot(null, i18nKey, tokenKey, null)), null);
    }

    private Property<TokenSnapshot> refreshDictionaryTokenSnapshot(Property<TokenSnapshot> dictionaryTokenProperty, Object freshI18nKey) {
        TokenSnapshot tokenSnapshot = dictionaryTokenProperty.getValue();
        Object i18nKey = tokenSnapshot.i18nKey;
        if (freshI18nKey == null)
            freshI18nKey = i18nKey;
        if (dictionaryLoadRequired && dictionaryLoader != null)
            scheduleMessageLoading(i18nKey, false);
        else {
            Dictionary dictionary = getDictionary();
            Object freshTokenValue = getFreshTokenValueFromSnapshot(tokenSnapshot, dictionary);
            if (!Objects.equals(tokenSnapshot.tokenValue, freshTokenValue) || tokenSnapshot.dictionary != dictionary || freshI18nKey != i18nKey) // Note freshI18nKey normally already equals i18nKey, but still may have an internal different state, so we use instance comparison
                dictionaryTokenProperty.setValue(new TokenSnapshot(dictionary, freshI18nKey, tokenSnapshot.tokenKey, freshTokenValue));
        }
        return dictionaryTokenProperty;
    }

    private Object getFreshTokenValueFromSnapshot(TokenSnapshot tokenSnapshot) {
        return getFreshTokenValueFromSnapshot(tokenSnapshot, tokenSnapshot.dictionary);
    }

    private Object getFreshTokenValueFromSnapshot(TokenSnapshot tokenSnapshot, Dictionary dictionary) {
        Object i18nKey = tokenSnapshot.i18nKey;
        TokenKey tokenKey = tokenSnapshot.tokenKey;
        return getDictionaryTokenValueImpl(i18nKey, (Enum<?> & TokenKey) tokenKey, dictionary, false, dictionary, false, true);
    }

    public boolean refreshMessageTokenProperties(Object freshI18nKey) {
        // Getting the message map to refresh
        Map<TokenKey, Reference<Property<TokenSnapshot>>> messageMap = liveDictionaryTokenProperties.get(freshI18nKey);
        // Note that the passed i18nKey may contain a fresher internal state than the one in token snapshots. For example,
        // if a message depends on another object such as the selected item (or Entity in Modality) in a context menu,
        // the i18nKey can be used to pass that object. This provider doesn't directly manage this case (i.e., use
        // the internal state of i18nKey to interpret the message), but some providers may extend this class to do so
        // by overriding getDictionaryTokenValueImpl() (ex: ModalityI18nProvider).
        // So we pass that fresh i18nKey to also ask to refresh the token snapshots with that fresh i18nKey.
        refreshMessageTokenSnapshots(messageMap, freshI18nKey);
        return messageMap != null; // reporting if something has been updated or not
    }

    @Override
    public void scheduleMessageLoading(Object i18nKey, boolean inDefaultLanguage) {
        if (blacklistedKeys.contains(i18nKey))
            return;
        // Adding the key to the keys to load
        Set<Object> keysToLoad = getKeysToLoad(inDefaultLanguage);
        keysToLoad.add(i18nKey);
        // Scheduling the dictionary loading if not already done
        if (!isDictionaryLoading()) {
            // Capturing the requested language (either current of default)
            Object language = inDefaultLanguage ? getDefaultLanguage() : getLanguage();
            // We schedule the load but defer it because we will probably have many successive calls to this method while
            // the application code is building the user interface. Only after collecting all the keys during these calls
            // (presumably in the same animation frame) do we do the actual load of these keys.
            dictionaryLoadingScheduled = UiScheduler.scheduleDeferred(() -> {
                // Making a copy of the keys before clearing it for the next possible schedule
                Set<Object> loadingKeys = new HashSet<>(keysToLoad); // ConcurrentModificationException observed
                keysToLoad.clear();
                // Extracting the message keys to load from them (in case they are different)
                Set<Object> messageKeysToLoad = loadingKeys.stream()
                    .map(this::i18nKeyToDictionaryMessageKey).collect(Collectors.toSet());
                // Asking the dictionary loader to load these messages in that language
                dictionaryLoader.loadDictionary(language, messageKeysToLoad)
                    .onFailure(e -> {
                        Console.log(e);
                        dictionaryProperty.set(null); // necessary to force default dictionary fallback and not keep previous language applied
                    })
                    .onSuccess(dictionary -> {
                        // Once the dictionary is loaded, we take it as the current dictionary if it's in the current language
                        if (language.equals(getLanguage())) // unless the load was a fallback to the default language
                            dictionaryProperty.setValue(dictionary);
                        // Also taking it as the default dictionary if it's in the default language
                        if (language.equals(getDefaultLanguage()))
                            defaultDictionaryProperty.setValue(dictionary);
                    })
                    .onComplete(ar -> {
                        // Turning off dictionaryLoadRequired
                        dictionaryLoadRequired = false;
                        // Refreshing all loaded keys in the user interface
                        Set<Object> unfoundKeys = null;
                        for (Object key : loadingKeys) {
                            boolean found = refreshMessageTokenProperties(key);
                            if (!found) {
                                if (unfoundKeys == null)
                                    unfoundKeys = new HashSet<>();
                                unfoundKeys.add(key);
                            }
                        }
                        if (unfoundKeys != null) {
                            blacklistedKeys.addAll(unfoundKeys);
                            Console.log("⚠️ I18n keys not found (now blacklisted): " + Collections.toStringCommaSeparated(unfoundKeys));
                        }
                        // If the requested language has changed in the meantime, we might need to reload another dictionary!
                        if (!language.equals(getLanguage())) {
                            // We postpone the call to be sure that dictionaryLoadingScheduled will be finished
                            UiScheduler.scheduleDeferred(this::onLanguageChanged);
                        }
                    })
                ;
            });
        }
    }

    private boolean isDictionaryLoading() {
        return dictionaryLoadingScheduled != null && !dictionaryLoadingScheduled.isFinished();
    }

    private Set<Object> getKeysToLoad(boolean inDefaultLanguage) {
        return inDefaultLanguage ? defaultKeysToLoad : keysToLoad;
    }

    private void refreshMessageTokenSnapshots(Map<TokenKey, Reference<Property<TokenSnapshot>>> messageMap, Object freshI18nKey) {
        if (messageMap != null)
            for (Iterator<Map.Entry<TokenKey, Reference<Property<TokenSnapshot>>>> it = messageMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<TokenKey, Reference<Property<TokenSnapshot>>> mapEntry = it.next();
                // Getting the tokenProperty through the reference
                Reference<Property<TokenSnapshot>> reference = mapEntry.getValue();
                Property<TokenSnapshot> tokenProperty = reference == null ? null : reference.get();
                // Although a tokenProperty is never null at initialization, it can be dropped by the GC since
                // it is contained in a WeakReference. If this happens, this means that the client software actually
                // doesn't use it (never from the beginning or just not anymore after an activity is closed, for
                // example), so we can just remove that entry to release some memory.
                if (tokenProperty == null) // Means the client software doesn't use this token
                    it.remove(); // So we can drop this entry
                else // Otherwise, the client software still uses it, and we need to update it
                    refreshDictionaryTokenSnapshot(tokenProperty, freshI18nKey);
            }
    }

    private void onLanguageChanged() {
        if (isDictionaryLoading())
            return;
        dictionaryLoadRequired = true;
        refreshAllLiveTokenSnapshots();
    }

    private synchronized void refreshAllLiveTokenSnapshots() {
        synchronized (liveDictionaryTokenProperties) {
            // We iterate through the translation map to update all parts (text, graphic, etc...) of all messages (i18nKey)
            for (Iterator<Map.Entry<Object, Map<TokenKey, Reference<Property<TokenSnapshot>>>>> it = liveDictionaryTokenProperties.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Object, Map<TokenKey, Reference<Property<TokenSnapshot>>>> messageMapEntry = it.next();
                refreshMessageTokenSnapshots(messageMapEntry.getValue(), null);
                // Although a message map is never empty at initialization, it can become empty if all i18nKey translationPart
                // have been removed (as explained above). If this happens, this means that the client software actually
                // doesn't use this message at all (either never from the beginning or not anymore).
                if (messageMapEntry.getValue().isEmpty()) // Means the client software doesn't use this i18nKey message
                    it.remove(); // So we can drop this entry
            }
        }
    }
}
