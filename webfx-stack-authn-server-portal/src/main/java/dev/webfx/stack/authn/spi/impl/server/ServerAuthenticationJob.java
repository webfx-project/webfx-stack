package dev.webfx.stack.authn.spi.impl.server;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.com.bus.BusService;
import dev.webfx.stack.com.bus.DeliveryOptions;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.session.state.server.ServerSideStateSessionSyncer;

/**
 * @author Bruno Salmon
 */
public class ServerAuthenticationJob implements ApplicationJob {

    private static final String LOCAL_BUS_ADDRESS = "bus/local/AuthenticationService/verifyAuthenticated";

    @Override
    public void onStart() {
        // We tell ServerSideStateSessionSyncer which userId checker to use, which is AuthenticationService.
        // But because ServerSideStateSessionSyncer is called from the bridge event handler, which happens in the http
        // server event loop thread, we invoke AuthenticationService through the event bus, like other client calls.
        // This is because AuthenticationService is not designed to be thread-safe, so all calls must originate
        // from the same thread, i.e., the main event loop thread.
        // For more explanation, see VertxBusModuleBooter (where the http server is set up).
        registerPushAuthorizationsOnEventBus();
        ServerSideStateSessionSyncer.setUserIdChecker(ignored -> callAuthenticationServiceOverEventBus());
    }

    private static Future<?> callAuthenticationServiceNowFromTheMainEventLoopThread() {
        return AuthenticationService.verifyAuthenticated();

    }

    private static void registerPushAuthorizationsOnEventBus() {
        BusService.bus().registerLocal(LOCAL_BUS_ADDRESS, message ->
            ThreadLocalStateHolder.runWithState(message.state(), () -> {
                callAuthenticationServiceNowFromTheMainEventLoopThread()
                    .onComplete(ar -> {
                        Object body;
                        if (ar.succeeded())
                            body = ar.result();
                        else
                            // body = AST.createReadOnlySingleKeyAstObject("failure", ar.cause().getMessage()); TODO: solve ClassCastException in AST.nativeToAstObject() when serialized over event bus
                            body = AST.createObject().set("failure", ar.cause().getMessage());
                        message.reply(body, DeliveryOptions.localOnlyDeliveryOptions());
                    });
            }));
    }

    private static Future<Object> callAuthenticationServiceOverEventBus() {
        Object state = ThreadLocalStateHolder.getThreadLocalState();
        Promise<Object> promise = Promise.promise();
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
                    promise.complete(body);
            }
        });
        return promise.future();
    }

}
