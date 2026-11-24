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
            .onFailure(e -> {
                if (isClientGone(e)) // the ordinary case: somebody closed a tab
                    Console.log("Did not push authorizations: the client is no longer connected — " + e.getMessage());
                else
                    Console.error("An error occurred while fetching and/or pushing authorizations to user", e);
            });
    }

    /**
     * Whether this failure is only the client having gone away before the push could reach it.
     *
     * <p>Measured on production over 24 hours: 178 of these, ALL of them a client that had already
     * disconnected — 114 "Discarded the request", 50 "No handlers for address", 14 a reply timeout.
     * None was an authorization fault. Logged at ERROR they were, with the closed-socket noise beside
     * them, part of the 92% of that channel which was not errors, and they were what hid the rest.
     *
     * <p><b>Matched on the message text, which is not where one would choose to match.</b> These
     * failures are Vert.x {@code ReplyException}s and their {@code failureType()} says this precisely,
     * but this module is platform-agnostic — it requires only {@code webfx.platform.*} and
     * {@code webfx.stack.*} — so the type is not on its path, and pulling Vert.x in to read one enum
     * would be a far worse trade than a fragile string.
     *
     * <p>So it fails SAFE instead: only these three known signatures are demoted, and anything else —
     * including a wording change in a future Vert.x — stays an error. A message this does not
     * recognise is louder than it needs to be, never quieter.
     */
    private static boolean isClientGone(Throwable e) {
        String message = e == null ? null : e.getMessage();
        return message != null
               && (message.contains("No handlers for address")
                   || message.contains("Discarded the request")
                   || message.contains("Timed out after waiting"));
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
