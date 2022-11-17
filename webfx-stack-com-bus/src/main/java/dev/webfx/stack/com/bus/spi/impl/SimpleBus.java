/*
 * Note: this code is a fork of Goodow realtime-channel project https://github.com/goodow/realtime-channel
 */

/*
 * Copyright 2013 Goodow.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package dev.webfx.stack.com.bus.spi.impl;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.com.bus.*;
import dev.webfx.platform.json.Json;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Handler;

import java.util.*;

/*
 * Simple bus implementation that can be used as a basis for more complex implementations. This simple implementation
 * doesn't know how to send messages over the network. So it works only as a single local bus instance that sends local
 * message only (to itself), or as a local node in a distributed environment that can handle received messages that
 * are passed to it. To make this bus able to send messages over the network itself (through the send() and publish()
 * methods), it must be extended. In particular, the doSendOrPublishImpl() method must be overridden to implement the
 * network delivery of non-local messages.
 *
 * Also, this implementation doesn't do any message encoding/decoding. So when receiving a raw message from the network,
 * the calling code is responsible for decoding that raw message first, and then passing the extracted address, body and
 * reply address to the onMessage() method.
 *
 * @author Bruno Salmon
 */
@SuppressWarnings("rawtypes")
public class SimpleBus implements Bus {

    private static final String ON_OPEN = "bus/onOpen";
    private static final String ON_CLOSE = "bus/onClose";
    private static final String ON_ERROR = "bus/onError";

    // List of handlers registered for local messages (sent or published locally on this bus instance)
    private final Map<String/*registered address*/, List<Handler<Message>>> localHandlerMap = new HashMap<>();
    // List of handlers registered for remote messages (received from another remote instance bus instance)
    private final Map<String/*registered address*/, List<Handler<Message>>> remoteHandlerMap = new HashMap<>();
    // List of reply handlers (will be automatically cleared once called)
    protected final Map<String/*reply address*/, Handler<AsyncResult<Message>>> replyHandlers = new HashMap<>();
    private final IdGenerator replyAddressGenerator = new IdGenerator();
    protected BusHook hook;
    private boolean open;

    public SimpleBus() {
        this(true);
    }

    public SimpleBus(boolean alreadyOpen) {
        // This simple bus is meant to be open all the time, so it doesn't publish open, close or error events by itself,
        // but more complex implementations can extend this class and publish such events.
        // Here, we are registering local handlers to intercept such events to just call onOpen(), onClose() and onError() methods.
        registerLocal(ON_OPEN,  msg -> onOpen());
        registerLocal(ON_CLOSE, msg -> onClose(msg.body())); // the message body should be the reason
        registerLocal(ON_ERROR, msg -> onError(msg.body())); // the message body should be the reason
        if (alreadyOpen)
            onOpen();
    }

    // Can be called by a more complex implementation to indicate the bus is open
    protected void publishOnOpenEvent() {
        publish(ON_OPEN, null, DeliveryOptions.LOCAL_ONLY);
    }

    // Can be called by a more complex implementation to indicate the bus is closed
    protected void publishOnCloseEvent(Object reason) {
        publish(ON_CLOSE, reason, DeliveryOptions.LOCAL_ONLY);
    }

    // Can be called by a more complex implementation to indicate the bus is on error
    protected void publishOnError(Object error) {
        publish(ON_ERROR, error, DeliveryOptions.LOCAL_ONLY);
    }

    // Reacting to an open event
    protected void onOpen() {
        open = true;
        Console.log("Bus open");
        if (hook != null)
            hook.handleOpened();
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    // Reacting to a close event
    protected void onClose(Object reason) {
        Console.log("Bus closed, reason = " + reason);
        open = false;
        clearHandlers();
        if (hook != null)
            hook.handlePostClose();
    }

    // Reacting to an error event
    protected void onError(Object reason) {
        Console.log("Bus error, reason = " + reason);
    }

    @Override
    public Bus setHook(BusHook hook) {
        this.hook = hook;
        return this;
    }

    @Override
    public void close() {
        if (hook == null || hook.handlePreClose())
            doClose();
    }

    protected void doClose() {
        publishOnCloseEvent(null);
    }

    // Publishing/sending message API


    @Override
    public Bus publish(String address, Object body, DeliveryOptions options) {
        return sendOrPublishImpl(false, address, body, options, null);
    }

    @Override
    public Bus send(String address, Object body, DeliveryOptions options) {
        return request(address, body, options, null);
    }

    @Override
    public <T> Bus request(String address, Object body, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
        return sendOrPublishImpl(true, address, body, options, replyHandler);
    }

    <T> Bus sendOrPublishImpl(boolean send, String address, Object body, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
        if (options.isLocalOnly() || hook == null || hook.handleSendOrPub(send, address, body, options, replyHandler))
            doSendOrPublishImpl(send, address, body, options, replyHandler);
        return this;
    }

    @SuppressWarnings("unchecked")
    protected <T> void doSendOrPublishImpl(boolean send, String address, Object body, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
        checkNotNull("address", address);
        // Registering the reply handler (if set)
        String replyAddress = registerReplyHandlerIfSet(replyHandler);
        // This implementation doesn't know how to send messages over the network, so the only thing that it can do is
        // to try delivering the message on the same bus instance (should work for local messages)
        boolean delivered = onMessage(send, address, replyAddress, body, options);
        // If the delivery failed, we unregister the reply handler (if set) as it will never be called!
        if (!delivered && replyAddress != null)
            unregisterReplyHandler(replyAddress);
    }

    protected <T> String registerReplyHandlerIfSet(Handler<AsyncResult<Message<T>>> replyHandler) {
        String replyAddress = null;
        if (replyHandler != null) {
            replyAddress = generateNewReplyAddress();
            replyHandlers.put(replyAddress, (Handler) replyHandler);
        }
        return replyAddress;
    }

    protected void unregisterReplyHandler(String replyAddress) {
        replyHandlers.remove(replyAddress);
    }

    // Message API. All incoming messages, either local, or from the network, should be passed to a onMessage() method,
    // that will try to deliver the message to a handler registered on this bus.

    protected boolean onMessage(boolean send, String address, String replyAddress, Object body, DeliveryOptions options) {
        // Embedding all the parameters into a single message object, and passing it to onMessage(Message).
        Message message = createMessage(send, address, replyAddress, body, options);
        return onMessage(message);
    }

    protected Message createMessage(boolean send, String address, String replyAddress, Object body, DeliveryOptions options) {
        return new SimpleMessage<>(send, this, address, replyAddress, body, options);
    }

    protected boolean onMessage(Message message) {
        // The hook may not allow the message delivery
        if (hook != null && !hook.handleReceiveMessage(message))
            return false;
        return doReceiveMessage(message);
    }

    private boolean doReceiveMessage(Message message) {
        String address = message.address();
        List<Handler<Message>> handlers = getHandlerMap(true).get(address);
        if (handlers == null)
            handlers = getHandlerMap(false).get(address);
        if (handlers != null) {
            // We make a copy since the handler might get unregistered from within the handler itself,
            // which would screw up our iteration
            List<Handler<Message>> copy = new ArrayList<>(handlers);
            // Drain any messages that came in while the channel was not open.
            for (Handler<Message> handler : copy)
                scheduleHandle(address, handler, message);
            return true;
        }
        // Might be a reply message
        Handler<AsyncResult<Message>> handler = replyHandlers.get(address);
        if (handler != null) {
            replyHandlers.remove(address);
            scheduleHandleAsync(address, handler, message);
            return true;
        }
        Console.log("Unknown message address: " + address);
        return false;
    }

    // Handler registration API

    protected Map<String, List<Handler<Message>>> getHandlerMap(boolean local) {
        return local ? localHandlerMap : remoteHandlerMap;
    }

    @Override
    public <T> Registration register(boolean local, String address, Handler<Message<T>> handler) {
        return registerImpl(local, address, handler);
    }

    protected boolean doRegister(boolean local, String address, Handler<? extends Message> handler) {
        checkNotNull("address", address);
        checkNotNull("handler", handler);
        Map<String, List<Handler<Message>>> handlerMap = getHandlerMap(local);
        List<Handler<Message>> handlers = handlerMap.get(address);
        if (handlers != null && handlers.contains(handler))
            return false;
        if (handlers == null)
            handlerMap.put(address, handlers = new ArrayList<>());
        handlers.add((Handler) handler);
        return true;
    }

    protected <T> boolean doUnregister(boolean local, String address, Handler<Message<T>> handler) {
        checkNotNull("address", address);
        checkNotNull("handler", handler);
        Map<String, List<Handler<Message>>> handlerMap = getHandlerMap(local);
        List<Handler<Message>> handlers = handlerMap.get(address);
        if (handlers == null)
            return false;
        boolean removed = handlers.remove(handler);
        if (handlers.isEmpty())
            handlerMap.remove(address);
        return removed;
    }

    void clearHandlers() {
        clearReplyHandlers();
        getHandlerMap(false).clear();
    }

    void clearReplyHandlers() {
        Iterator<Map.Entry<String, Handler<AsyncResult<Message>>>> it = replyHandlers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Handler<AsyncResult<Message>>> entry = it.next();
            if (shouldClearReplyHandlerNow(entry.getKey())) {
                entry.getValue().handle(Future.failedFuture(new Exception("Bus closed")));
                it.remove();
            }
        }
    }

    protected boolean shouldClearReplyHandlerNow(String replyAddress) {
        return true;
    }

    protected String generateNewReplyAddress() {
        return replyAddressGenerator.next(36);
    }

    private void handle(String address, Handler<AsyncResult<Message>> handler, Message message) {
        //Console.log("handle(), address = " + address + ", handler = " + handler + ", message = " + message);
        try {
            handler.handle(Future.succeededFuture(message));
        } catch (Throwable e) {
            Console.log("Failed to handle on address: " + address, e);
            publish(ON_ERROR, Json.createObject().set("address", address).set("message", message).set("cause", e), DeliveryOptions.LOCAL_ONLY);
        }
    }

    private void scheduleHandle(String address, Handler<Message> handler, Message message) {
        scheduleHandleAsync(address, ar -> {
            if (ar.failed())
                Console.log(ar.cause());
            else
                handler.handle(ar.result());
        }, message);
    }

    private void scheduleHandleAsync(String address, Handler<AsyncResult<Message>> handler, Message message) {
        //Console.log("scheduleHandle(), address = " + address + ", handler = " + handler + ", message = " + message);
        if (message.options().isLocalOnly())
            handle(address, handler, message);
        else
            Scheduler.scheduleDeferred(() -> handle(address, handler, message));
    }

    private <T> Registration registerImpl(boolean local, String address, Handler<Message<T>> handler) {
        if (hook == null || hook.handlePreRegister(address, handler))
            doRegister(local, address, handler);
        return () -> doUnregister(local, address, handler);
    }

    protected static void checkNotNull(String paramName, Object param) {
        if (param == null)
            throw new IllegalArgumentException("Parameter " + paramName + " must be specified");
    }

}