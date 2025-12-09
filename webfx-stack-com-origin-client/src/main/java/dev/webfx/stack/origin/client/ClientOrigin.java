package dev.webfx.stack.origin.client;

import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.util.Strings;
import dev.webfx.platform.windowlocation.WindowLocation;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class ClientOrigin {

    public static String getClientAppBaseUrl() {
        return Strings.removeSuffix(WindowLocation.getHref(), "#" + WindowLocation.getFragment());
    }

    public static String getClientAppRouteUrl(String routePath) {
        return getClientAppBaseUrl() + "#" + routePath;
    }

    public static String getHttpServerRestUrl(String restPath) {
        return getHttpServerOrigin() + restPath;
    }

    public static String getHttpServerOrigin() {
        String origin = evaluateOrNull("${{ HTTP_SERVER_ORIGIN }}");
        if (origin == null)
            origin = "https://" + evaluateOrNull("${{ HTTP_SERVER_HOST | BUS_SERVER_HOST | SERVER_HOST }}");
        return origin;
    }

    private static String evaluateOrNull(String expression) {
        String value = ConfigLoader.getRootConfig().get(expression);
        if (Objects.equals(value, expression))
            value = null;
        return value;
    }
}
