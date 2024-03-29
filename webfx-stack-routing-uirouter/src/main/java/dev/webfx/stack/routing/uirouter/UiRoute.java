package dev.webfx.stack.routing.uirouter;

import dev.webfx.stack.routing.activity.Activity;
import dev.webfx.stack.routing.activity.ActivityContextFactory;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.routing.router.util.PathBuilder;
import dev.webfx.stack.routing.uirouter.impl.UiRouteImpl;
import dev.webfx.platform.util.collection.Collections;
import dev.webfx.platform.util.function.Converter;
import dev.webfx.platform.util.function.Factory;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public interface UiRoute<C extends UiRouteActivityContext<C>> {

    String getPath();

    boolean isRegex();

    default boolean requiresAuthentication() {
        return false;
    }

    Factory<Activity<C>> activityFactory();

    default ActivityContextFactory<C> activityContextFactory() {
        return null;
    }

    default Converter<RoutingContext, C> contextConverter() {
        return null;
    }


    static <C extends UiRouteActivityContext<C>> UiRoute<C> create(String path, boolean auth, Factory<Activity<C>> activityFactory, ActivityContextFactory<C> activityContextFactory) {
        return create(path, false, auth, activityFactory, activityContextFactory, null);
    }

    static <C extends UiRouteActivityContext<C>> UiRoute<C> createRegex(String path, boolean auth, Factory<Activity<C>> activityFactory, ActivityContextFactory<C> activityContextFactory) {
        return create(PathBuilder.toRegexPath(path), true, auth, activityFactory, activityContextFactory, null);
    }

    static <C extends UiRouteActivityContext<C>> UiRoute<C> create(String path, boolean regex, boolean auth, Factory<Activity<C>> activityFactory, ActivityContextFactory<C> activityContextFactory) {
        return create(path, regex, auth, activityFactory, activityContextFactory, null);
    }

    static <C extends UiRouteActivityContext<C>> UiRoute<C> create(String path, boolean regex, boolean auth, Factory<Activity<C>> activityFactory, ActivityContextFactory<C> activityContextFactory, Converter<RoutingContext, C> contextConverter) {
        return new UiRouteImpl<>(path, regex, auth, activityFactory, activityContextFactory, contextConverter);
    }

    static Collection<UiRoute> getProvidedUiRoutes() {
        return Collections.listOf(ServiceLoader.load(UiRoute.class));
    }
}
