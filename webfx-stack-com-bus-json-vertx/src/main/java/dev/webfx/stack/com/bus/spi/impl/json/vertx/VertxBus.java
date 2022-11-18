package dev.webfx.stack.com.bus.spi.impl.json.vertx;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Handler;
import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.spi.impl.listmap.MapBasedJsonObject;
import dev.webfx.platform.vertx.common.VertxInstance;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusHook;
import dev.webfx.stack.com.bus.Message;
import dev.webfx.stack.com.bus.Registration;
import dev.webfx.stack.com.bus.spi.impl.json.JsonBusConstants;
import dev.webfx.stack.com.bus.spi.impl.json.server.ServerJsonBusStateManager;
import dev.webfx.stack.session.spi.impl.vertx.VertxSession;
import dev.webfx.stack.session.state.StateAccessor;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.impl.EventBusInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.web.Session;

/**
 * @author Bruno Salmon
 */
final class VertxBus implements Bus {

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
            if (incomingMessage || outgoingMessage) {
                JsonObject rawMessage = bridgeEvent.getRawMessage();
                if (rawMessage != null) {
                    Session webSession = bridgeEvent.socket().webSession();
                    VertxSession vertxSession = VertxSession.create(webSession);
                    // This is the main call for state management
                    Future<Boolean> sessionStorageFuture =
                            ServerJsonBusStateManager.manageStateOnIncomingOrOutgoingRawJsonMessage(Json.createObject(rawMessage), vertxSession, incomingMessage);
                    if (incomingMessage && !sessionStorageFuture.isComplete()) {
                        callBridgeEventComplete = false;
                        sessionStorageFuture.onComplete(x -> bridgeEvent.complete(true));
                    }
                }
            }
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
            body = Json.createObject();
        if (body instanceof MapBasedJsonObject) {
            MapBasedJsonObject webfxBody = (MapBasedJsonObject) body;
            body = webfxBody.getNativeElement();
        }
        return body;
    }

    private static Object vertxToWebfxBody(Object body) {
        Object object = body;
        if (object instanceof JsonObject)
            object = Json.createObject(object);
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
