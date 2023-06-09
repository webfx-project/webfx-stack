package dev.webfx.stack.i18n.controls;

import dev.webfx.stack.i18n.I18n;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputControl;

/**
 * @author Bruno Salmon
 */
public final class I18nControls {

    public static <T extends Labeled> T setI18nProperties(T labeled, Object i18nKey) {
        labeled.setText(I18n.getI18nText(i18nKey));
        labeled.setGraphic(I18n.getI18nGraphic(i18nKey));
        return labeled;
    }

    public static <T extends Labeled> T bindI18nProperties(T labeled, Object i18nKey) {
        I18n.bindI18nTextProperty(labeled.textProperty(), i18nKey);
        I18n.bindI18nGraphicProperty(labeled.graphicProperty(), i18nKey);
        return labeled;
    }

    public static <T extends TextInputControl> T bindI18nProperties(T textInputControl, Object i18nKey) {
        I18n.bindI18nPromptProperty(textInputControl.promptTextProperty(), i18nKey);
        return textInputControl;
    }

    public static <T extends Tab> T bindI18nProperties(T tab, Object i18nKey) {
        I18n.bindI18nTextProperty(tab.textProperty(), i18nKey);
        I18n.bindI18nGraphicProperty(tab.graphicProperty(), i18nKey);
        return tab;
    }
}
