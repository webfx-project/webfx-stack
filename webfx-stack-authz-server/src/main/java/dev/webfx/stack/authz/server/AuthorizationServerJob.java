package dev.webfx.stack.authz.server;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.session.state.server.ServerSideStateSessionSyncer;

/**
 * @author Bruno Salmon
 */
public class AuthorizationServerJob implements ApplicationJob {

    private static final String LOCAL_BUS_ADDRESS = "bus/local/AuthorizationServerService/pushAuthorizations";

    @Override
    public void onStart() {
        // We tell ServerSideStateSessionSyncer which userId authorizer to use, which is AuthorizationServerService.
        // But because ServerSideStateSessionSyncer is called from the bridge event handler, which happens in the http
        // server event loop thread, we invoke AuthorizationServerService through the event bus, like other client calls.
        // This is because AuthorizationServerService is not designed to be thread-safe, so all calls must originate
        // from the same thread, i.e., the main event loop thread.
        // For more explanation, see VertxBusModuleBooter (where the http server is set up).
        registerAuthorizationServerServiceOnEventBus();
        ServerSideStateSessionSyncer.setUserIdAuthorizer(ignored -> callAuthorizationServerServiceOverEventBus());
    }

    private static Future<Void> callAuthorizationServerServiceNowFromMainEventLoopThread() {
        // We push the authorizations associated with the userId to the client (identified by runId). It's important
        // to first set these 2 parameters (userId and runId) in ThreadLocalStateHolder before calling this method.
        // This responsibility is fulfilled by ServerSideStateSessionSyncer.
        return AuthorizationServerService.pushAuthorizations()
            .onFailure(e -> Console.error("An error occurred while fetching and/or pushing authorizations to user", e));
    }

    private static void registerAuthorizationServerServiceOnEventBus() {
        BusService.bus().registerLocal(LOCAL_BUS_ADDRESS, message ->
            ThreadLocalStateHolder.runWithState(message.state(), () -> {
                callAuthorizationServerServiceNowFromMainEventLoopThread()
                    .onComplete(ar -> {
                        Object body;
                        if (ar.succeeded())
                            body = ar.result();
                        else
                            body = AST.createReadOnlySingleKeyAstObject("failure", ar.cause().getMessage());
                        message.reply(body, DeliveryOptions.localOnlyDeliveryOptions());
                    });
            }));
    }

    private static Future<Void> callAuthorizationServerServiceOverEventBus() {
        Object state = ThreadLocalStateHolder.getThreadLocalState();
        Promise<Void> promise = Promise.promise();
        BusService.bus().request(LOCAL_BUS_ADDRESS, null, DeliveryOptions.localOnlyDeliveryOptions(state), ar -> {
            if (ar.failed())
                promise.fail(ar.cause());
            else {
                Object body = ar.result().body();
                String failure = null;
                if (AST.isObject(body))
                    failure = ((ReadOnlyAstObject) body).getString("failure");
                if (failure != null)
                    promise.fail(failure);
                else
                    promise.complete();
            }
        });
        return promise.future();
    }
}
