package dev.webfx.stack.routing.uirouter.operations;

import dev.webfx.platform.util.serviceloader.MultipleServiceProviders;
import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public interface RouteRequestEmitter {

    RouteRequest instantiateRouteRequest(UiRouteActivityContext context);

    static Collection<RouteRequestEmitter> getProvidedEmitters() {
        return MultipleServiceProviders.getProviders(RouteRequestEmitter.class, () -> ServiceLoader.load(RouteRequestEmitter.class));
    }
}
