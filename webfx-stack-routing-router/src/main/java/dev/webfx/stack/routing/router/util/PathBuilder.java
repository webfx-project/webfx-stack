package dev.webfx.stack.routing.router.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Bruno Salmon
 */
public final class PathBuilder {

    private final static Pattern PATH_WITH_SEMI_COLON_PARAMETER_PATTERN = Pattern.compile(".*(:([^/|)]+)).*");

    public static String toRegexPath(String path) {
        while (true) {
            Matcher matcher = PATH_WITH_SEMI_COLON_PARAMETER_PATTERN.matcher(path);
            if (!matcher.matches())
                break;
            path = path.replace(matcher.group(1),"(?<" + matcher.group(2) + ">[^/]*)");
        }
        return path;
    }

    public static String toRegexOrPath(String... paths) {
        return toRegexPath(String.join("|", paths));
    }

}
