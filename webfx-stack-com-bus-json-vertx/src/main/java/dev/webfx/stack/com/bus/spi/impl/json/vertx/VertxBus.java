package dev.webfx.stack.com.bus.spi.impl.json.vertx;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Handler;
import dev.webfx.platform.vertx.common.VertxInstance;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusHook;
import dev.webfx.stack.com.bus.Message;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.com.bus.spi.impl.json.JsonBusConstants;
import dev.webfx.stack.com.bus.spi.impl.json.server.ServerJsonBusStateManager;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.state.StateAccessor;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.impl.EventBusInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;

/**
 * @author Bruno Salmon
 */
final class VertxBus implements Bus {

    private static final boolean REPLY_PONG_TO_PING = true;

    private final EventBus eventBus;
    private boolean open = true;
    private BusHook busHook;

    VertxBus(EventBus eventBus) {
        this.eventBus = eventBus;
        // Initialising state management
        ServerJsonBusStateManager.initialiseStateManagement(this);
        // Also intercepting the incoming and outgoing json messages for the state management
        VertxInstance.setBridgeEventHandler(bridgeEvent -> {
            boolean callBridgeEventComplete = true;
            BridgeEventType type = bridgeEvent.type();
            // Incoming messages (from client to server): type = send or publish
            boolean incomingMessage = type.equals(BridgeEventType.SEND) || type.equals(BridgeEventType.PUBLISH);
            // Outgoing messages  (from server to client): type = receive
            boolean outgoingMessage = type.equals(BridgeEventType.RECEIVE);
            boolean ping = type.equals(BridgeEventType.SOCKET_PING);
            // We get the web session. It is based on cookies, so 2 different tabs in the same browser share the same
            // web session, which is annoying, we don't want to mix sessions, as each tab can be a different application
            // (ex: back-office, front-office, magic link, etc...), and each communicates with the server with its own
            // web socket connection. So we will use the socket itself as an identifier of the session.
            SockJSSocket socket = bridgeEvent.socket();
            Session vertxWebSession = socket.webSession();
            // We will use the socket uri as the identifier, as it's unique per client (something like /eventbus/568/rzhmtc04/websocket)
            String socketUri = socket.uri();
            // And we will use the web session to store "inside" each possible client session running under that same
            // browser. It's possible that 1 client disconnect and reconnect, which will produce 2 sessions inside (as
            // the second websocket is a new one), but the second session should retrieve the data of the first as they
            // will have actually the same serverSessionId (re-communicated by the client).
            // So we retrieve that session from the web session, or create a new session if we can't find it.
            dev.webfx.stack.session.Session webfxSession = vertxWebSession.get(socketUri);
            if (webfxSession == null) {
                webfxSession = SessionService.getSessionStore().createSession();
                vertxWebSession.put(socketUri, webfxSession);
            }

            if (ping) {
                ServerJsonBusStateManager.clientIsLive(null, webfxSession);
                if (REPLY_PONG_TO_PING)
                    socket.write("{\"type\":\"pong\"}");
            } else if (incomingMessage || outgoingMessage) {
                JsonObject rawMessage = bridgeEvent.getRawMessage();
                if (rawMessage != null) {
                    // This is the main call for state management
                    Future<?> sessionFuture = ServerJsonBusStateManager.manageStateOnIncomingOrOutgoingRawJsonMessage(
                            AST.createObject(rawMessage), webfxSession, incomingMessage)
                        .onSuccess(finalSession -> vertxWebSession.put(socketUri, finalSession));
                    // If the session is not ready right now (this may happen because of a session switch), then
                    // we need to wait this operation to complete before continuing the message delivery
                    if (incomingMessage && !sessionFuture.isComplete()) {
                        callBridgeEventComplete = false;
                        sessionFuture.onComplete(x -> bridgeEvent.complete(true));
                    }
                }
            }
            // If the session is ready right now, we continue the message delivery right now
            if (callBridgeEventComplete)
                bridgeEvent.complete(true);
        });
    }

    private static Object getMessageState(io.vertx.core.eventbus.Message<?> message) {
        return StateAccessor.decodeState(message.headers().get(JsonBusConstants.HEADERS_STATE));
    }

    private static DeliveryOptions webfxToVertxDeliveryOptions(dev.webfx.stack.com.bus.DeliveryOptions webfxOptions) {
        DeliveryOptions deliveryOptions = new DeliveryOptions().setLocalOnly(webfxOptions.isLocalOnly());
        Object state = webfxOptions.getState();
        if (state != null)
            deliveryOptions.addHeader(JsonBusConstants.HEADERS_STATE, StateAccessor.encodeState(state));
        deliveryOptions.setSendTimeout(3*60 * 1000); // Temporarily set to 3 mins instead of 30s
        return deliveryOptions;
    }

    @Override
    public void close() {
        if (eventBus instanceof EventBusInternal) {
            Promise<Void> promise = Promise.promise();
            ((EventBusInternal) eventBus).close(promise);
            promise.future().onSuccess(e -> open = false);
        } else
            open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public Bus publish(String address, Object body, dev.webfx.stack.com.bus.DeliveryOptions options) {
        eventBus.publish(address, webfxToVertxBody(body), webfxToVertxDeliveryOptions(options));
        return this;
    }

    @Override
    public Bus send(String address, Object body, dev.webfx.stack.com.bus.DeliveryOptions options) {
        eventBus.send(address, webfxToVertxBody(body), webfxToVertxDeliveryOptions(options));
        return this;
    }

    @Override
    public <T> Bus request(String address, Object body, dev.webfx.stack.com.bus.DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
        eventBus.<T>request(address, webfxToVertxBody(body), webfxToVertxDeliveryOptions(options), ar -> replyHandler.handle(vertxToWebfxMessageAsyncResult(ar, options.isLocalOnly())));
        return this;
    }

    @Override
    public Bus setHook(BusHook busHook) {
        this.busHook = busHook;
        return this;
    }

    public <T> Registration register(boolean local, String address, Handler<Message<T>> handler) {
        MessageConsumer<T> consumer = local ? eventBus.localConsumer(address) : eventBus.consumer(address);
        consumer.handler(message -> handler.handle(vertxToWebfxMessage(message, local)));
        return consumer::unregister;
    }

    private static Object webfxToVertxBody(Object body) {
        if (body == null)
            body = AST.createObject();
        // TODO: check if we can generify this with AST
        if (AST.NATIVE_FACTORY != null && AST.isObject(body)) {
            body = AST.NATIVE_FACTORY.astToNativeObject((ReadOnlyAstObject) body);
        }
        return body;
    }

    private static Object vertxToWebfxBody(Object body) {
        Object object = body;
        // TODO: check if we can generify this with AST
        if (AST.NATIVE_FACTORY != null && AST.NATIVE_FACTORY.acceptAsNativeObject(body)) {
            object = AST.NATIVE_FACTORY.nativeToAstObject(body);
        }
        return object;
    }

    private static <T> AsyncResult<Message<T>> vertxToWebfxMessageAsyncResult(io.vertx.core.AsyncResult<io.vertx.core.eventbus.Message<T>> ar, boolean local) {
        if (ar.failed())
            return Future.failedFuture(ar.cause());
        return Future.succeededFuture(vertxToWebfxMessage(ar.result(), local));
    }

    private static <T> Message<T> vertxToWebfxMessage(io.vertx.core.eventbus.Message<T> vertxMessage, boolean local) {
        return new Message<>() {

            private dev.webfx.stack.com.bus.DeliveryOptions options;

            @Override
            public T body() {
                return (T) vertxToWebfxBody(vertxMessage.body());
            }

            @Override
            public void fail(int failureCode, String msg) {
                vertxMessage.fail(failureCode, msg);
            }

            @Override
            public void reply(Object body, dev.webfx.stack.com.bus.DeliveryOptions options) {
                vertxMessage.reply(webfxToVertxBody(body), webfxToVertxDeliveryOptions(options));
            }

            @Override
            public <T1> void reply(Object body, dev.webfx.stack.com.bus.DeliveryOptions options, Handler<AsyncResult<Message<T1>>> replyHandler) {
                vertxMessage.<T1>replyAndRequest(webfxToVertxBody(body), webfxToVertxDeliveryOptions(options), ar -> replyHandler.handle(vertxToWebfxMessageAsyncResult(ar, false)));
            }

            @Override
            public dev.webfx.stack.com.bus.DeliveryOptions options() {
                if (options == null)
                    options = new dev.webfx.stack.com.bus.DeliveryOptions().setLocalOnly(local).setState(getMessageState(vertxMessage));
                return options;
            }

            @Override
            public String replyAddress() {
                return vertxMessage.replyAddress();
            }

            @Override
            public String address() {
                return vertxMessage.address();
            }
        };
    }
}
