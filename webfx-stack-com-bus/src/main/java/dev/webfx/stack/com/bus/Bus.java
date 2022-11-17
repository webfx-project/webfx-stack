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

package dev.webfx.stack.com.bus;

import dev.webfx.platform.async.Handler;
import dev.webfx.platform.async.AsyncResult;

/**
 * A distributed lightweight event bus which can encompass multiple machines. The event bus
 * implements publish/register, point to point messaging and request-response messaging.<p>
 * Messages sent over the event bus are represented by instances of the {@link Message} class.<p>
 * For publish/register, messages can be published to an address using one of the {@link #register}
 * methods. An address is a simple {@code String} instance.<p>
 * Handlers are registered against an address. There can be multiple handlers registered against
 * each address, and a particular handler can be registered against multiple addresses. The event
 * bus will route an incoming message to all handlers which are registered against that address.<p>
 * For point to point messaging, messages can be sent to an address using one of the {@link #request}
 * methods. The messages will be delivered to a single handler, if one is registered on that
 * address. If more than one handler is registered on the same address, the bus will choose one and
 * deliver the message to that. The bus will aim to fairly distribute messages in a round-robin way,
 * but does not guarantee strict round-robin under all circumstances.<p>
 * The order of messages received by any specific handler from a specific sender should match the
 * order of messages sent from that sender.<p>
 * When sending a message, a reply handler can be provided. If so, it will be called when the reply
 * from the receiver has been received. Reply messages can also be replied to, etc... ad infinitum<p>
 * Different event bus instances can be clustered together over a network, to give a single logical
 * event bus.<p>
 *
 * @author 田传武 (aka Larry Tin) - author of Goodow realtime-channel project
 * @author Bruno Salmon - fork, refactor & update for the webfx project
 *
 * <a href="https://github.com/goodow/realtime-channel/blob/master/src/main/java/com/goodow/realtime/channel/Bus.java">Original Goodow class</a>
 */

public interface Bus {

    /**
     * Publish a message
     *
     * @param address The address to publish it to
     * @param body    The message body
     */
    default Bus publish(String address, Object body) {
        return publish(address, body, new DeliveryOptions());
    }

    /**
     * Publish a message with delivery options
     *
     * @param address The address to publish it to
     * @param body    The message body
     * @param options The delivery options
     */
    Bus publish(String address, Object body, DeliveryOptions options);

    /**
     * Send a message
     *
     * @param address      The address to send it to
     * @param body         The message body
     */
    default Bus send(String address, Object body) {
        return send(address, body, new DeliveryOptions());
    }

    /**
     * Send a message with delivery options
     *
     * @param address The address to publish it to
     * @param body    The message body
     * @param options The delivery options
     */
    Bus send(String address, Object body, DeliveryOptions options);

    /**
     * Send a message and wait for a reply
     *
     * @param address      The address to send it to
     * @param body         The message body
     * @param replyHandler Reply handler will be called when any reply from the recipient is received
     */
    default <T> Bus request(String address, Object body, Handler<AsyncResult<Message<T>>> replyHandler) {
        return request(address, body, new DeliveryOptions(), replyHandler);
    }

    /**
     * Send a message and wait for a reply
     *
     * @param address      The address to send it to
     * @param body         The message body
     * @param replyHandler Reply handler will be called when any reply from the recipient is received
     */
    <T> Bus request(String address, Object body, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler);

    /**
     * Registers a handler against the specified address
     *
     * @param address The address to register it at
     * @param handler The handler
     * @return the handler registration, can be stored in order to unregister the handler later
     */
    default <T> Registration register(String address, Handler<Message<T>> handler) {
        return register(false, address, handler);
    }

    /**
     * Registers a local handler against the specified address. The handler info won't be propagated
     * across the cluster
     *
     * @param address The address to register it at
     * @param handler The handler
     */
    default <T> Registration registerLocal(String address, Handler<Message<T>> handler) {
        return register(true, address, handler);
    }

    /**
     * Registers a handler against the specified address
     *
     * @param local   Indicates if the address is local or propagated across the cluster
     * @param address The address to register it at
     * @param handler The handler
     * @return the handler registration, can be stored in order to unregister the handler later
     */
    <T> Registration register(boolean local, String address, Handler<Message<T>> handler);

    /**
     * Close the Bus and release all resources.
     */
    void close();

    boolean isOpen();

    /**
     * Set a BusHook on the Bus
     *
     * @param hook The hook
     */
    Bus setHook(BusHook hook);
}