package dev.webfx.stack.framework.client.operations.route;

import dev.webfx.stack.framework.client.activity.impl.elementals.uiroute.UiRouteActivityContext;
import dev.webfx.stack.framework.shared.router.auth.authz.RouteRequest;
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
