package dev.webfx.stack.i18n;

/**
 * @author Bruno Salmon
 */
public final class I18nKeys {

    public static String appendEllipsis(String i18nKey) {
        return i18nKey + "...";
    }

    public static String appendColons(String i18nKey) {
        return i18nKey + ":";
    }

    public static String appendArrows(String i18nKey) {
        return i18nKey + ">>";
    }

    public static String prependArrows(String i18nKey) {
        return "<<" + i18nKey;
    }

}
