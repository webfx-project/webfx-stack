package dev.webfx.stack.i18n.spi.impl;

import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.Strings;
import dev.webfx.stack.i18n.DefaultTokenKey;
import dev.webfx.stack.i18n.Dictionary;
import dev.webfx.stack.i18n.TokenKey;
import dev.webfx.stack.i18n.spi.I18nProvider;
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

    private static class TokenSnapshot {
        private final Dictionary dictionary;
        private final Object i18nKey;
        private final TokenKey tokenKey;
        private final Object tokenValue;

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
                    value = tokenSnapshot.tokenValue;
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
    private Set<Object> unloadedKeys, unloadedDefaultKeys;

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
    }

    private Object guessDefaultLanguage() {
        return getSupportedLanguages().stream().findFirst().orElse(null);
    }

    private Object guessInitialLanguage() {
        return null;
    }

    private final ObjectProperty<Object> languageProperty = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            onLanguageChanged();
        }
    };

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
        return getDictionaryTokenValueImpl(i18nKey, tokenKey, dictionary, false, false, false);
    }

    protected <TK extends Enum<?> & TokenKey> Object getDictionaryTokenValueImpl(Object i18nKey, TK tokenKey, Dictionary dictionary, boolean skipDefaultDictionary, boolean skipMessageKeyInterpretation, boolean skipMessageLoading) {
        Object tokenValue = null;
        if (dictionary != null && i18nKey != null) {
            Object messageKey = i18nKeyToDictionaryMessageKey(i18nKey);
            tokenValue = dictionary.getMessageTokenValue(messageKey, tokenKey);
            if (tokenValue == null && !skipMessageKeyInterpretation && messageKey instanceof String) {
                String sKey = (String) messageKey;
                int length = Strings.length(sKey);
                if (length > 1) {
                    int index = 0;
                    while (index < length && !Character.isLetterOrDigit(sKey.charAt(index)))
                        index++;
                    if (index > 0) {
                        String prefix = sKey.substring(0, index);
                        switch (prefix) {
                            case "<<":
                                // Reading the token value of the remaining key (after <<)
                                tokenValue = getDictionaryTokenValueImpl(new I18nSubKey(sKey.substring(prefix.length(), length), i18nKey), tokenKey, dictionary, skipDefaultDictionary, false, skipMessageLoading);
                                if (tokenValue != null && isAssignableFrom(tokenKey.expectedClass(), String.class))
                                    tokenValue = "" + getDictionaryTokenValueImpl(prefix, tokenKey, dictionary, skipDefaultDictionary, true, skipMessageLoading) + tokenValue;
                        }
                    }
                }
                if (tokenValue == null && length > 1) {
                    int index = length;
                    while (index > 0 && !Character.isLetterOrDigit(sKey.charAt(index - 1)))
                        index--;
                    if (index < length) {
                        String suffix = sKey.substring(index, length);
                        switch (suffix) {
                            case ":":
                            case "?":
                            case ">>":
                            case "...":
                                // Reading the token value of the remaining key (before the suffix)
                                tokenValue = getDictionaryTokenValueImpl(new I18nSubKey(sKey.substring(0, length - suffix.length()), i18nKey), tokenKey, dictionary, skipDefaultDictionary, true, skipMessageLoading);
                                if (tokenValue != null && isAssignableFrom(tokenKey.expectedClass(), String.class))
                                    tokenValue = "" + tokenValue + getDictionaryTokenValueImpl(suffix, tokenKey, dictionary, skipDefaultDictionary, true, skipMessageLoading);
                        }
                    }
                }
            }
            if (tokenValue instanceof String || tokenValue == null && messageKey instanceof String) {
                String sToken = (String) (tokenValue == null ? messageKey : tokenValue);
                int i1 = sToken.indexOf('[');
                if (i1 >= 0) {
                    int i2 = i1 == 0 && sToken.endsWith("]") ? sToken.length() - 1 : sToken.indexOf(']', i1 + 1);
                    if (i2 > 0) {
                        Object resolvedValue = getDictionaryTokenValueImpl(new I18nSubKey(sToken.substring(i1 + 1, i2), i18nKey), tokenKey, dictionary, false, false, skipMessageLoading);
                        // If the bracket token has been resolved, we return it with the parts before and after the brackets
                        if (resolvedValue != null)
                            tokenValue = (i1 == 0 ? "" : sToken.substring(0, i1 - 1)) + resolvedValue + sToken.substring(i2 + 1);
                    }
                }
            }
            if (tokenValue == null && !skipDefaultDictionary) {
                Dictionary defaultDictionary = getDefaultDictionary();
                if (dictionary != defaultDictionary && defaultDictionary != null)
                    tokenValue = getDictionaryTokenValueImpl(i18nKey, tokenKey, defaultDictionary, true, skipMessageKeyInterpretation, skipMessageLoading); //getI18nPartValue(tokenSnapshot.i18nKey, part, defaultDictionary, skipPrefixOrSuffix);
                if (tokenValue == null) {
                    if (!skipMessageLoading)
                        scheduleMessageLoading(i18nKey, true);
                    if (tokenKey == DefaultTokenKey.TEXT)
                        tokenValue = messageKey; //;whatToReturnWhenI18nTextIsNotFound(tokenSnapshot.i18nKey, tokenSnapshot.tokenKey);
                }
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
        return refreshDictionaryTokenSnapshot(new SimpleObjectProperty<>(new TokenSnapshot(null, i18nKey, tokenKey, null)));
    }

    private Property<TokenSnapshot> refreshDictionaryTokenSnapshot(Property<TokenSnapshot> dictionaryTokenProperty) {
        TokenSnapshot tokenSnapshot = dictionaryTokenProperty.getValue();
        Object i18nKey = tokenSnapshot.i18nKey;
        if (dictionaryLoadRequired && dictionaryLoader != null)
            scheduleMessageLoading(i18nKey, false);
        else {
            Dictionary dictionary = getDictionary();
            TokenKey tokenKey = tokenSnapshot.tokenKey;
            Object freshTokenValue = getDictionaryTokenValueImpl(i18nKey, (Enum<?> & TokenKey) tokenKey, dictionary, false, false, true);
            if (!Objects.equals(tokenSnapshot.tokenValue, freshTokenValue) || tokenSnapshot.dictionary != dictionary)
                dictionaryTokenProperty.setValue(new TokenSnapshot(dictionary, i18nKey, tokenKey, freshTokenValue));
        }
        return dictionaryTokenProperty;
    }

    public void refreshMessageTokenProperties(Object i18nKey) {
        refreshMessageTokenSnapshots(liveDictionaryTokenProperties.get(i18nKey));
    }

    @Override
    public void scheduleMessageLoading(Object i18nKey, boolean inDefaultLanguage) {
        Set<Object> unloadedI18nKeys = getUnloadedKeys(inDefaultLanguage);
        if (unloadedI18nKeys != null)
            unloadedI18nKeys.add(i18nKey);
        else {
            setUnloadedKeys(unloadedI18nKeys = new HashSet<>(), inDefaultLanguage);
            unloadedI18nKeys.add(i18nKey);
            UiScheduler.scheduleDeferred(() -> {
                Object language = inDefaultLanguage ? getDefaultLanguage() : getLanguage();
                Set<Object> loadingI18nKeys = getUnloadedKeys(inDefaultLanguage);
                Set<Object> loadingMessageKeys = loadingI18nKeys.stream() // Possible NPE observed!
                        .map(this::i18nKeyToDictionaryMessageKey).collect(Collectors.toSet()); // TODO: fix possible ConcurrentModificationException
                dictionaryLoader.loadDictionary(language, loadingMessageKeys)
                        .onSuccess(dictionary -> {
                            if (!inDefaultLanguage)
                                dictionaryProperty.setValue(dictionary); // TODO: fix possible java.lang.NullPointerException: Cannot invoke "javafx.beans.value.ChangeListener.changed(javafx.beans.value.ObservableValue, Object, Object)" because "<local3>[<local7>]" is null
                            if (language.equals(getDefaultLanguage()))
                                defaultDictionaryProperty.setValue(dictionary);
                            dictionaryLoadRequired = false;
                            for (Object key : loadingI18nKeys)
                                refreshMessageTokenProperties(key);
                        });
                setUnloadedKeys(null, inDefaultLanguage);
            });
        }
    }

    private Set<Object> getUnloadedKeys(boolean inDefaultLanguage) {
        return inDefaultLanguage ? unloadedDefaultKeys : unloadedKeys;
    }

    private void setUnloadedKeys(Set<Object> unloadedKeys, boolean inDefaultLanguage) {
        if (inDefaultLanguage)
            unloadedDefaultKeys = unloadedKeys;
        else
            this.unloadedKeys = unloadedKeys;
    }

    private void refreshMessageTokenSnapshots(Map<TokenKey, Reference<Property<TokenSnapshot>>> messageMap) {
        if (messageMap != null)
            for (Iterator<Map.Entry<TokenKey, Reference<Property<TokenSnapshot>>>> it = messageMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<TokenKey, Reference<Property<TokenSnapshot>>> mapEntry = it.next();
                // Getting the tokenProperty through the reference
                Reference<Property<TokenSnapshot>> reference = mapEntry.getValue();
                Property<TokenSnapshot> tokenProperty = reference == null ? null : reference.get();
                // Although a tokenProperty is never null at initialization, it can be dropped by the GC since
                // it is contained in a WeakReference. If this happens, this means that the client software actually
                // doesn't use it (either never from the beginning or just not anymore after an activity is closed
                // for example), so we can just remove that entry to release some memory.
                if (tokenProperty == null) // Means the client software doesn't use this token
                    it.remove(); // So we can drop this entry
                else // Otherwise, the client software still uses it, and we need to update it
                    refreshDictionaryTokenSnapshot(tokenProperty);
            }
    }

    private void onLanguageChanged() {
        dictionaryLoadRequired = true;
        refreshAllLiveTokenSnapshots();
    }

    private synchronized void refreshAllLiveTokenSnapshots() {
        synchronized (liveDictionaryTokenProperties) {
            // We iterate through the translation map to update all parts (text, graphic, etc...) of all messages (i18nKey)
            for (Iterator<Map.Entry<Object, Map<TokenKey, Reference<Property<TokenSnapshot>>>>> it = liveDictionaryTokenProperties.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Object, Map<TokenKey, Reference<Property<TokenSnapshot>>>> messageMapEntry = it.next();
                refreshMessageTokenSnapshots(messageMapEntry.getValue());
                // Although a message map is never empty at initialization, it can become empty if all i18nKey,translationPart
                // have been removed (as explained above). If this happens, this means that the client software actually
                // doesn't use this message at all (either never from the beginning or not anymore).
                if (messageMapEntry.getValue().isEmpty()) // Means the client software doesn't use this i18nKey message
                    it.remove(); // So we can drop this entry
            }
        }
    }

    /*private String whatToReturnWhenI18nTextIsNotFound(Object i18nKey, I18nPart part) {
        String value = Strings.toString(i18nKeyToDictionaryMessageKey(i18nKey));
        return interpretDictionaryValue(i18nKey, part, value);
    }*/

}
