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

    public static String upperCase(String i18nKey) {
        return i18nKey.toUpperCase();
    }

    public static String lowerCase(String i18nKey) {
        return i18nKey.toLowerCase();
    }

    public static String upperCaseFirstChar(String i18nKey) {
        char firstCharKey = i18nKey.charAt(0);
        if (!Character.isUpperCase(firstCharKey))
            i18nKey = Character.toUpperCase(firstCharKey) + i18nKey.substring(1);
        return i18nKey;
    }

    public static String lowerCaseFirstChar(String i18nKey) {
        char firstCharKey = i18nKey.charAt(0);
        if (!Character.isLowerCase(firstCharKey))
            i18nKey = Character.toLowerCase(firstCharKey) + i18nKey.substring(1);
        return i18nKey;
    }

    public static String embedInString(String i18nKey) {
        return "[" + i18nKey + "]";
    }

    public static String embedInString(String text, String i18nKey) {
        return text.replace("[0]", embedInString(i18nKey));
    }

}
