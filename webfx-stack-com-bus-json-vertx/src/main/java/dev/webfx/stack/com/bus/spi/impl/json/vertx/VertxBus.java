package dev.webfx.stack.com.bus.spi.impl.json.vertx;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Handler;
import dev.webfx.platform.util.Strings;
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

    VertxBus(EventBus eventBus) {
        this.eventBus = eventBus;
        // Initialising state management
        ServerJsonBusStateManager.initialiseStateManagement(this);
        // Also intercepting the incoming and outgoing JSON messages for the state management
        VertxInstance.setBridgeEventHandler(bridgeEvent -> {
            boolean callBridgeEventComplete = true;
            BridgeEventType type = bridgeEvent.type();
            // Incoming messages (from client to server): type = send or publish
            boolean isIncomingMessage = type.equals(BridgeEventType.SEND) || type.equals(BridgeEventType.PUBLISH);
            // Outgoing messages (from server to client): type = receive
            boolean isOutgoingMessage = type.equals(BridgeEventType.RECEIVE);
            boolean isPing = type.equals(BridgeEventType.SOCKET_PING);
            // We get the web session. It is based on cookies, so 2 different tabs in the same browser share the same
            // web session, which is annoying, we don't want to mix sessions, as each tab can be a different application
            // (ex: back-office, front-office, etc...), and each communicates with the server with its own web socket
            // connection. So we will use the socket itself as an identifier of the session.
            SockJSSocket socket = bridgeEvent.socket();
            Session vertxWebSession = socket.webSession();
            // We will use the socket uri as the identifier, as it's unique per client (something like /eventbus/568/rzhmtc04/websocket)
            String socketUri = socket.uri();
            // And we will use the web session to store "inside" each possible client session running under that same
            // browser. It's possible that 1 client disconnect and reconnect, which will produce 2 sessions inside (as
            // the second websocket is new). However, the second session should retrieve the data of the first as they
            // will have actually the same serverSessionId (re-communicated by the client).
            // So we retrieve that session from the web session or create a new session if we can't find it.
            dev.webfx.stack.session.Session webfxSession = vertxWebSession.get(socketUri);
            if (webfxSession == null) {
                webfxSession = SessionService.getSessionStore().createSession();
                vertxWebSession.put(socketUri, webfxSession);
            }

            if (isPing) { // receiving or sending a ping (note: there is no way to distinguish receiving or sending)
                // When receiving a ping from the client, we reply with a simple pong message
                if (REPLY_PONG_TO_PING)
                    socket.write("{\"type\":\"pong\"}");
                // and also indicate the state manager that the client is live
                ServerJsonBusStateManager.clientIsLive(null, webfxSession);
                // When sending a ping, we don't enrich the client state. This includes when the server pushes a new
                // state to the client (via a ping with headers), such as when the user authenticates; the endpoint
                // has already defined precisely the state to send. This ping state may even be delivered to another
                // client (ex: during a magic link authentication, the original session is also authenticated).
                // So it's very important to not enrich this state with the original client state.
            } else if (isIncomingMessage || isOutgoingMessage) { // message exchange between client and server
                JsonObject rawMessage = bridgeEvent.getRawMessage();
                if (rawMessage != null) {
                    AstObject astMessage = AST.createObject(rawMessage);
                    // What we want to achieve here when intercepting such messages is to automatically manage the state
                    // of these incoming and outgoing messages. The state is enriched with information known about the
                    // client, such as its sessionId, userId, runId when it's appropriate to communicate them. They are
                    // communicated either to the final server endpoint point (for incoming messages) or back to the
                    // client through a reply (for outgoing messages) after a possible change made by the endpoint,
                    // which can result in an update of the client (ex: login or logout).

                    // Detection of incoming endpoints (external clients to server, then redirected to a server local endpoint)
                    boolean isIncomingEndpoint = isIncomingMessage && Strings.startsWith(astMessage.getString(JsonBusConstants.ADDRESS), "busCallService");

                    // Detection of outgoing unicast: we use the "unicast" header, which is set to true when the server
                    // replies to a client or requests a specific client (see reply() & request() implementations below).
                    AstObject astHeaders = astMessage.get(JsonBusConstants.HEADERS);
                    boolean isOutgoingUnicast = isOutgoingMessage && astHeaders != null && "true".equals(astHeaders.remove(JsonBusConstants.HEADERS_UNICAST));
                    // Note: it's very important to communicate the outgoing state only when unicasting private messages
                    // to a specific client. Otherwise, the other clients would consider this state to be their own,
                    // causing them a login switch or a logout!
                    if (isIncomingEndpoint || isOutgoingUnicast) {
                        // This is the main call for state management
                        Future<?> sessionFuture = ServerJsonBusStateManager.manageStateOnIncomingOrOutgoingRawJsonMessage(
                                astMessage, webfxSession, isIncomingMessage)
                            .onSuccess(finalSession -> vertxWebSession.put(socketUri, finalSession));
                        // If the session is not ready right now (this may happen because of a session switch), then
                        // we need to wait this operation to complete before continuing the message delivery
                        if (isIncomingMessage && !sessionFuture.isComplete()) {
                            callBridgeEventComplete = false;
                            sessionFuture.onComplete(x -> bridgeEvent.complete(true));
                        }
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

    private static DeliveryOptions webfxToVertxDeliveryOptions(dev.webfx.stack.com.bus.DeliveryOptions webfxOptions, boolean unicast) {
        DeliveryOptions deliveryOptions = new DeliveryOptions().setLocalOnly(webfxOptions.isLocalOnly());
        Object state = webfxOptions.getState();
        if (state != null)
            deliveryOptions.addHeader(JsonBusConstants.HEADERS_STATE, StateAccessor.encodeState(state));
        if (unicast)
            deliveryOptions.addHeader(JsonBusConstants.HEADERS_UNICAST, "true");
        deliveryOptions.setSendTimeout(3*60 * 1000); // Temporarily set to 3 mins instead of 30 s
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
        eventBus.publish(address, webfxToVertxBody(body), webfxToVertxDeliveryOptions(options, false));
        return this;
    }

    @Override
    public Bus send(String address, Object body, dev.webfx.stack.com.bus.DeliveryOptions options) {
        eventBus.send(address, webfxToVertxBody(body), webfxToVertxDeliveryOptions(options, false));
        return this;
    }

    @Override
    public <T> Bus request(String address, Object body, dev.webfx.stack.com.bus.DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
        eventBus.<T>request(address, webfxToVertxBody(body), webfxToVertxDeliveryOptions(options, true), ar -> replyHandler.handle(vertxToWebfxMessageAsyncResult(ar, options.isLocalOnly())));
        return this;
    }

    @Override
    public Bus setHook(BusHook busHook) {
        // No usage so far on the server side, so we don't implement it
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
                vertxMessage.reply(webfxToVertxBody(body), webfxToVertxDeliveryOptions(options, true));
            }

            @Override
            public <T1> void reply(Object body, dev.webfx.stack.com.bus.DeliveryOptions options, Handler<AsyncResult<Message<T1>>> replyHandler) {
                vertxMessage.<T1>replyAndRequest(webfxToVertxBody(body), webfxToVertxDeliveryOptions(options, true), ar -> replyHandler.handle(vertxToWebfxMessageAsyncResult(ar, false)));
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
