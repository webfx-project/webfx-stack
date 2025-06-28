package dev.webfx.stack.i18n;

/**
 * @author Bruno Salmon
 */
public final class I18nKeys {

    public static String appendEllipsis(Object i18nKey) {
        return i18nKey + "...";
    }

    public static String appendColons(Object i18nKey) {
        return i18nKey + ":";
    }

    public static String appendArrows(Object i18nKey) {
        return i18nKey + ">>";
    }

    public static String prependArrows(Object i18nKey) {
        return "<<" + i18nKey;
    }

    public static String upperCase(Object i18nKey) {
        return i18nKey.toString().toUpperCase();
    }

    public static String lowerCase(Object i18nKey) {
        return i18nKey.toString().toLowerCase();
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

    public static String embedInString(Object i18nKey) {
        return "[" + i18nKey + "]";
    }

    public static String embedInString(String text, Object i18nKey) {
        return text.replace("[0]", embedInString(i18nKey));
    }

}
