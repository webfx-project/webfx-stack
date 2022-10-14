package dev.webfx.stack.i18n.spi;

import dev.webfx.platform.util.collection.Collections;
import dev.webfx.stack.i18n.Dictionary;
import dev.webfx.stack.i18n.TokenKey;
import dev.webfx.stack.i18n.operations.ChangeLanguageRequestEmitter;
import dev.webfx.stack.ui.fxraiser.FXRaiser;
import dev.webfx.stack.ui.fxraiser.FXValueRaiser;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public interface I18nProvider {

    default Collection<Object> getSupportedLanguages() {
        return Collections.map(getProvidedInstantiators(), i -> i.emitLanguageRequest().getLanguage());
    }

    default Collection<ChangeLanguageRequestEmitter> getProvidedInstantiators() {
        return Collections.listOf(ServiceLoader.load(ChangeLanguageRequestEmitter.class));
    }

    ObjectProperty<Object> languageProperty();
    default Object getLanguage() { return languageProperty().getValue(); }
    default void setLanguage(Object language) { languageProperty().setValue(language); }

    ObservableObjectValue<Dictionary> dictionaryProperty();
    default Dictionary getDictionary() {
        return dictionaryProperty().getValue();
    }

    Object getDefaultLanguage();
    Dictionary getDefaultDictionary();

    /// NEW API

    default <TK extends Enum<?> & TokenKey> Object getDictionaryTokenValue(Object i18nKey, TK tokenKey) {
        return getDictionaryTokenValue(i18nKey, tokenKey, null);
    }

    default <TK extends Enum<?> & TokenKey> Object getDictionaryTokenValue(Object i18nKey, TK tokenKey, Dictionary dictionary) {
        if (dictionary == null)
            dictionary = getDictionary();
        return dictionary.getMessageTokenValue(i18nKeyToDictionaryMessageKey(i18nKey), tokenKey);
    }

    // Temporary (should be protected)
    default Object i18nKeyToDictionaryMessageKey(Object i18nKey) {
        if (i18nKey instanceof HasDictionaryMessageKey)
            return ((HasDictionaryMessageKey) i18nKey).getDictionaryMessageKey();
        return i18nKey;
    }

    default <TK extends Enum<?> & TokenKey> Object getUserTokenValue(Object i18nKey, TK tokenKey, Object... args) {
        return getUserTokenValue(i18nKey, tokenKey, getDictionary(), args);
    }

    default <TK extends Enum<?> & TokenKey> Object getUserTokenValue(Object i18nKey, TK tokenKey, Dictionary dictionary, Object... args) {
        Object dictionaryValue = getDictionaryTokenValue(i18nKey, tokenKey, dictionary);
        return FXRaiser.raiseToObject(dictionaryValue, tokenKey.expectedClass(), getI18nFxValueRaiser(), args);
    }

    <TK extends Enum<?> & TokenKey> ObservableValue<?> dictionaryTokenProperty(Object i18nKey, TK tokenKey, Object... args);

    default <TK extends Enum<?> & TokenKey> ObservableValue<?> userTokenProperty(Object i18nKey, TK tokenKey, Object... args) {
        return FXRaiser.raiseToProperty(dictionaryTokenProperty(i18nKey, tokenKey, args), tokenKey.expectedClass(), getI18nFxValueRaiser(), args);
    }

    default FXValueRaiser getI18nFxValueRaiser() {
        return null;
    }

    void refreshMessageTokenProperties(Object i18nKey);

    void scheduleMessageLoading(Object i18nKey, boolean inDefaultLanguage);


}
