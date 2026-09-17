package dev.webfx.stack.http.server.vertx;

import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.vertx.VertxInstance;

/**
 * @author Bruno Salmon
 */
public final class VertxHttpStarterJob implements ApplicationJob {

    private String verticleDeployID;

    @Override
    public void onStart() {
        VertxHttpRouterConfigurator.finaliseRouter();
        VertxInstance.getVertx().deployVerticle(new VertxHttpVerticle())
                .onFailure(e -> Console.log("❌ Error while deploying VertxHttpVerticle: " + e.getMessage()))
                .onSuccess(deployID -> verticleDeployID = deployID);
    }

    @Override
    public void onStop() {
        // Tell the load balancer we're draining: /health now returns 503 so the ALB stops routing
        VertxHttpRouterConfigurator.serverReady = false;
        if (verticleDeployID != null)
            VertxInstance.getVertx().undeploy(verticleDeployID);
    }
}
