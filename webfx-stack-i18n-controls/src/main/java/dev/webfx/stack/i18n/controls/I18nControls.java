package dev.webfx.stack.i18n.controls;

import dev.webfx.stack.i18n.I18n;
import javafx.scene.control.*;

/**
 * @author Bruno Salmon
 */
public final class I18nControls {

    public static <T extends Labeled> T setI18nProperties(T labeled, Object i18nKey, Object... args) {
        setI18nTextProperty(labeled, i18nKey, args);
        return setI18nGraphicProperty(labeled, i18nKey, args);
    }

    public static <T extends Labeled> T setI18nTextProperty(T labeled, Object i18nKey, Object... args) {
        labeled.setText(I18n.getI18nText(i18nKey, args));
        return labeled;
    }

    public static <T extends Labeled> T setI18nGraphicProperty(T labeled, Object i18nKey, Object... args) {
        labeled.setGraphic(I18n.getI18nGraphic(i18nKey, args));
        return labeled;
    }

    public static <T extends Labeled> T bindI18nProperties(T labeled, Object i18nKey, Object... args) {
        bindI18nTextProperty(labeled, i18nKey, args);
        bindI18nGraphicProperty(labeled, i18nKey, args);
        bindI18nTextFillProperty(labeled, i18nKey, args);
        return labeled;
    }

    public static <T extends Labeled> T bindI18nTextProperty(T labeled, Object i18nKey, Object... args) {
        I18n.bindI18nTextProperty(labeled.textProperty(), i18nKey, args);
        return labeled;
    }

    public static <T extends Labeled> T bindI18nGraphicProperty(T labeled, Object i18nKey, Object... args) {
        I18n.bindI18nGraphicProperty(labeled.graphicProperty(), i18nKey, args);
        return labeled;
    }

    public static <T extends Labeled> T bindI18nTextFillProperty(T labeled, Object i18nKey, Object... args) {
        I18n.bindI18nTextFillProperty(labeled.textFillProperty(), i18nKey, args);
        return labeled;
    }

    public static <T extends TextInputControl> T bindI18nProperties(T textInputControl, Object i18nKey, Object... args) {
        return bindI18nPromptProperty(textInputControl, i18nKey, args);
    }

    public static <T extends TextInputControl> T bindI18nPromptProperty(T textInputControl, Object i18nKey, Object... args) {
        I18n.bindI18nPromptProperty(textInputControl.promptTextProperty(), i18nKey, args);
        return textInputControl;
    }

    public static <T extends Tab> T bindI18nProperties(T tab, Object i18nKey, Object... args) {
        bindI18nTextProperty(tab, i18nKey, args);
        return bindI18nGraphicProperty(tab, i18nKey, args);
    }

    public static <T extends Tab> T bindI18nTextProperty(T tab, Object i18nKey, Object... args) {
        I18n.bindI18nTextProperty(tab.textProperty(), i18nKey, args);
        return tab;
    }

    public static <T extends Tab> T bindI18nGraphicProperty(T tab, Object i18nKey, Object... args) {
        I18n.bindI18nGraphicProperty(tab.graphicProperty(), i18nKey, args);
        return tab;
    }

    public static Label newLabel(Object i18nKey, Object... args) {
        return bindI18nProperties(new Label(), i18nKey, args);
    }

    public static Button newButton(Object i18nKey, Object... args) {
        return bindI18nProperties(new Button(), i18nKey, args);
    }

    public static RadioButton newRadioButton(Object i18nKey, Object... args) {
        return bindI18nProperties(new RadioButton(), i18nKey, args);
    }

    public static Hyperlink newHyperlink(Object i18nKey, Object... args) {
        return bindI18nProperties(new Hyperlink(), i18nKey, args);
    }

    public static CheckBox newCheckBox(Object i18nKey, Object... args) {
        return bindI18nProperties(new CheckBox(), i18nKey, args);
    }

    public static ToggleButton newToggleButton(Object i18nKey, Object... args) {
        return bindI18nProperties(new ToggleButton(), i18nKey, args);
    }

}
