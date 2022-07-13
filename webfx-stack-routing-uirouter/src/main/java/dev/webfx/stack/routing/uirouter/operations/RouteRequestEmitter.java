package dev.webfx.stack.routing.uirouter.operations;

import dev.webfx.stack.routing.uirouter.activity.uiroute.UiRouteActivityContext;
import dev.webfx.stack.routing.router.auth.authz.RouteRequest;
import dev.webfx.platform.util.collection.Collections;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public interface RouteRequestEmitter {

    RouteRequest instantiateRouteRequest(UiRouteActivityContext context);

    static Collection<RouteRequestEmitter> getProvidedEmitters() {
        return Collections.listOf(ServiceLoader.load(RouteRequestEmitter.class));
    }
}
