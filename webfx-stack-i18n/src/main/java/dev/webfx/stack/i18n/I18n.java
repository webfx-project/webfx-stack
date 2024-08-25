package dev.webfx.stack.i18n;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.i18n.spi.I18nProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Node;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class I18n {

    static {
        SingleServiceProvider.registerServiceSupplier(I18nProvider.class, () -> ServiceLoader.load(I18nProvider.class));
    }

    public static I18nProvider getProvider() {
        return SingleServiceProvider.getProvider(I18nProvider.class);
    }

    public static List<Object> getSupportedLanguages() {
        return getProvider().getSupportedLanguages();
    }

    public static ObjectProperty<Object> languageProperty() {
        return getProvider().languageProperty();
    }

    public static Object getLanguage() {
        return getProvider().getLanguage();
    }

    public static Object getDefaultLanguage() {
        return getProvider().getDefaultLanguage();
    }

    public static void setLanguage(Object language) {
        getProvider().setLanguage(language);
    }

    public static ObservableObjectValue<Dictionary> dictionaryProperty() {
        return getProvider().dictionaryProperty();
    }

    public static Dictionary getDictionary() {
        return getProvider().getDictionary();
    }

    // Generic String API

    public static <TK extends Enum<?> & TokenKey> String getStringTokenValue(Object i18nKey, TK tokenKey, Object... args) {
        return (String) getProvider().getUserTokenValue(i18nKey, tokenKey, args);
    }

    public static <TK extends Enum<?> & TokenKey> ObservableStringValue getStringTokenProperty(Object i18nKey, TK tokenKey, Object... args) {
        return (ObservableStringValue) getProvider().userTokenProperty(i18nKey, tokenKey, args);
    }

    public static <TK extends Enum<?> & TokenKey> void bindI18nStringProperty(Property<String> stringProperty, Object i18nKey, TK tokenKey, Object... args) {
        // Old commented code (immediate unconditional binding):
        // stringProperty.bind(getStringTokenProperty(i18nKey, tokenKey, args));

        // Now it's a lazy conditional binding: we do the binding only if we find an entry in the i18n dictionary.
        // For example, if the developer wants textFill to be managed by i18n for a specific message, he will add this
        // textFill entry in the dictionary and I18n will do the binding, but if he wants the textFill to be set by the
        // application code or CSS instead, he will omit this entry from the dictionary and I18n won't do the binding.
        ObservableStringValue stringTokenProperty = getStringTokenProperty(i18nKey, tokenKey, args);
        FXProperties.onPropertySet(stringTokenProperty, x -> stringProperty.bind(stringTokenProperty));
    }

    // Generic Object API

    public static <T, TK extends Enum<?> & TokenKey> T getObjectTokenValue(Object i18nKey, TK tokenKey, Object... args) {
        return (T) getProvider().getUserTokenValue(i18nKey, tokenKey, args);
    }

    public static <T, TK extends Enum<?> & TokenKey> ObservableObjectValue<T> getObjectTokenProperty(Object i18nKey, TK tokenKey, Object... args) {
        return (ObservableObjectValue<T>) getProvider().userTokenProperty(i18nKey, tokenKey, args);
    }

    public static <T, TK extends Enum<?> & TokenKey> void bindI18nObjectProperty(Property<T> objectProperty, Object i18nKey, TK tokenKey, Object... args) {
        // Old commented code (immediate unconditional binding):
        // objectProperty.bind(getObjectTokenProperty(i18nKey, tokenKey, args));

        // Now it's a lazy conditional binding (same explanation as bindI18nStringProperty()).
        ObservableObjectValue<T> objectTokenProperty = getObjectTokenProperty(i18nKey, tokenKey, args);
        FXProperties.onPropertySet(objectTokenProperty, x -> objectProperty.bind(objectTokenProperty));
    }

    // Text token API

    public static String getI18nText(Object i18nKey, Object... args) {
        return getStringTokenValue(i18nKey, DefaultTokenKey.TEXT, args);
    }

    public static ObservableStringValue i18nTextProperty(Object i18nKey, Object... args) {
        return getStringTokenProperty(i18nKey, DefaultTokenKey.TEXT, args);
    }

    public static void bindI18nTextProperty(Property<String> textProperty, Object i18nKey, Object... args) {
        bindI18nStringProperty(textProperty, i18nKey, DefaultTokenKey.TEXT, args);
    }

    // Prompt token API

    public static String getI18nPrompt(Object i18nKey, Object... args) {
        return getStringTokenValue(i18nKey, DefaultTokenKey.PROMPT, args);
    }

    public static ObservableStringValue i18nPromptProperty(Object i18nKey, Object... args) {
        return getStringTokenProperty(i18nKey, DefaultTokenKey.PROMPT, args);
    }

    public static void bindI18nPromptProperty(Property<String> promptProperty, Object i18nKey, Object... args) {
        bindI18nStringProperty(promptProperty, i18nKey, DefaultTokenKey.PROMPT, args);
    }

    // Graphic token API

    public static Node getI18nGraphic(Object i18nKey, Object... args) {
        return getObjectTokenValue(i18nKey, DefaultTokenKey.GRAPHIC, args);
    }

    public static ObservableObjectValue<Node> i18nGraphicProperty(Object i18nKey, Object... args) {
        return getObjectTokenProperty(i18nKey, DefaultTokenKey.GRAPHIC, args);
    }

    public static void bindI18nGraphicProperty(Property<Node> graphicProperty, Object i18nKey, Object... args) {
        bindI18nObjectProperty(graphicProperty, i18nKey, DefaultTokenKey.GRAPHIC, args);
    }

    // Fill token API

    public static Paint getI18nFill(Object i18nKey, Object... args) {
        return getObjectTokenValue(i18nKey, DefaultTokenKey.FILL, args);
    }

    public static ObservableObjectValue<Paint> i18nFillProperty(Object i18nKey, Object... args) {
        return getObjectTokenProperty(i18nKey, DefaultTokenKey.FILL, args);
    }

    public static void bindI18nFillProperty(Property<Paint> fillProperty, Object i18nKey, Object... args) {
        bindI18nObjectProperty(fillProperty, i18nKey, DefaultTokenKey.FILL, args);
    }


    // TextFill token API

    public static Paint getI18nTextFill(Object i18nKey, Object... args) {
        return getObjectTokenValue(i18nKey, DefaultTokenKey.TEXT_FILL, args);
    }

    public static ObservableObjectValue<Paint> i18nTextFillProperty(Object i18nKey, Object... args) {
        return getObjectTokenProperty(i18nKey, DefaultTokenKey.TEXT_FILL, args);
    }

    public static void bindI18nTextFillProperty(Property<Paint> textFillProperty, Object i18nKey, Object... args) {
        bindI18nObjectProperty(textFillProperty, i18nKey, DefaultTokenKey.TEXT_FILL, args);
    }


    public static void refreshMessageTokenProperties(Object i18nKey) {
        getProvider().refreshMessageTokenProperties(i18nKey);
    }

    // Controls API

    public static <T extends Text> T bindI18nProperties(T text, Object i18nKey, Object... args) {
        bindI18nTextProperty(text.textProperty(), i18nKey, args);
        return text;
    }

}
