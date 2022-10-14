package dev.webfx.stack.i18n;

import dev.webfx.platform.util.serviceloader.SingleServiceProvider;
import dev.webfx.stack.i18n.spi.I18nProvider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputControl;
import javafx.scene.text.Text;

import java.util.Collection;
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

    public static Collection<Object> getSupportedLanguages() {
        return getProvider().getSupportedLanguages();
    }

    public static ObjectProperty<Object> languageProperty() {
        return getProvider().languageProperty();
    }

    public static Object getLanguage() {
        return getProvider().getLanguage();
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
        stringProperty.bind(getStringTokenProperty(i18nKey, tokenKey, args));
    }

    // Generic Node API

    public static <TK extends Enum<?> & TokenKey> Node getNodeTokenValue(Object i18nKey, TK tokenKey, Object... args) {
        return (Node) getProvider().getUserTokenValue(i18nKey, tokenKey, args);
    }

    public static <TK extends Enum<?> & TokenKey> ObservableObjectValue<Node> getNodeTokenProperty(Object i18nKey, TK tokenKey, Object... args) {
        return (ObservableObjectValue<Node>) getProvider().userTokenProperty(i18nKey, tokenKey, args);
    }

    public static <TK extends Enum<?> & TokenKey> void bindI18nNodeProperty(Property<Node> nodeProperty, Object i18nKey, TK tokenKey, Object... args) {
        nodeProperty.bind(getNodeTokenProperty(i18nKey, tokenKey, args));
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
        return getNodeTokenValue(i18nKey, DefaultTokenKey.GRAPHIC, args);
    }

    public static ObservableObjectValue<Node> i18nGraphicProperty(Object i18nKey, Object... args) {
        return getNodeTokenProperty(i18nKey, DefaultTokenKey.GRAPHIC, args);
    }

    public static void bindI18nGraphicProperty(Property<Node> graphicProperty, Object i18nKey, Object... args) {
        bindI18nNodeProperty(graphicProperty, i18nKey, DefaultTokenKey.GRAPHIC, args);
    }

    public static void refreshMessageTokenProperties(Object i18nKey) {
        getProvider().refreshMessageTokenProperties(i18nKey);
    }

    // Controls API

    public static <T extends Text> T bindI18nProperties(T text, Object i18nKey) {
        bindI18nTextProperty(text.textProperty(), i18nKey);
        return text;
    }

    public static <T extends Labeled> T setI18nProperties(T labeled, Object i18nKey) {
        labeled.setText(getI18nText(i18nKey));
        labeled.setGraphic(getI18nGraphic(i18nKey));
        return labeled;
    }

    public static <T extends Labeled> T bindI18nProperties(T labeled, Object i18nKey) {
        bindI18nTextProperty(labeled.textProperty(), i18nKey);
        return labeled;
    }

    public static <T extends TextInputControl> T bindI18nProperties(T textInputControl, Object i18nKey) {
        bindI18nPromptProperty(textInputControl.promptTextProperty(), i18nKey);
        return textInputControl;
    }

    public static <T extends Tab> T bindI18nProperties(T tab, Object i18nKey) {
        bindI18nTextProperty(tab.textProperty(), i18nKey);
        bindI18nGraphicProperty(tab.graphicProperty(), i18nKey);
        return tab;
    }

}
