package dev.webfx.stack.routing.uirouter.impl;

import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.router.RoutingContext;
import dev.webfx.stack.routing.uirouter.UiRoute;
import dev.webfx.stack.routing.activity.Activity;
import dev.webfx.stack.routing.activity.ActivityContextFactory;
import dev.webfx.platform.util.function.Converter;
import dev.webfx.platform.util.function.Factory;

/**
 * @author Bruno Salmon
 */
public class UiRouteImpl<C extends UiRouteActivityContext<C>> implements UiRoute<C> {

    private final String path;
    private final boolean regex;
    private final boolean auth;
    private final Factory<Activity<C>> activityFactory;
    private final ActivityContextFactory<C> activityContextFactory;
    private final Converter<RoutingContext, C> contextConverter;

    public UiRouteImpl(UiRoute<C> uiRoute) {
        this(uiRoute.getPath(), uiRoute.isRegex(), uiRoute.requiresAuthentication(), uiRoute.activityFactory(), uiRoute.activityContextFactory(), uiRoute.contextConverter());
    }

    public UiRouteImpl(String path, boolean regex, boolean auth, Factory<Activity<C>> activityFactory, ActivityContextFactory<C> activityContextFactory, Converter<RoutingContext, C> contextConverter) {
        this.path = path;
        this.regex = regex;
        this.auth = auth;
        this.activityFactory = activityFactory;
        this.activityContextFactory = activityContextFactory;
        this.contextConverter = contextConverter;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isRegex() {
        return regex;
    }

    @Override
    public boolean requiresAuthentication() {
        return auth;
    }

    @Override
    public Factory<Activity<C>> activityFactory() {
        return activityFactory;
    }

    @Override
    public ActivityContextFactory<C> activityContextFactory() {
        return activityContextFactory;
    }

    @Override
    public Converter<RoutingContext, C> contextConverter() {
        return contextConverter;
    }
}
