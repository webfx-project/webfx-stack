package dev.webfx.stack.framework.client.ui.uirouter;

import dev.webfx.stack.framework.client.activity.Activity;
import dev.webfx.stack.framework.client.activity.ActivityContextFactory;
import dev.webfx.stack.framework.client.activity.impl.elementals.uiroute.UiRouteActivityContext;
import dev.webfx.stack.framework.shared.router.RoutingContext;
import dev.webfx.stack.framework.shared.router.util.PathBuilder;
import dev.webfx.stack.framework.client.ui.uirouter.impl.UiRouteImpl;
import dev.webfx.platform.shared.util.collection.Collections;
import dev.webfx.platform.shared.util.function.Converter;
import dev.webfx.platform.shared.util.function.Factory;

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
