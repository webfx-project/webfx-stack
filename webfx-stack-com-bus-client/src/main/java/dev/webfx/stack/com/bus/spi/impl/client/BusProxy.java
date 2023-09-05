/*
 * Note: this code is a fork of Goodow realtime-channel project https://github.com/goodow/realtime-channel
 */

/*
 * Copyright 2014 Goodow.com
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
package dev.webfx.stack.com.bus.spi.impl.client;


import dev.webfx.stack.com.bus.*;
import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Handler;

/*
 * @author 田传武 (aka Larry Tin) - author of Goodow realtime-channel project
 * @author Bruno Salmon - fork, refactor & update for the webfx project
 *
 * <a href="https://github.com/goodow/realtime-channel/blob/master/src/main/java/com/goodow/realtime/channel/impl/BusProxy.java">Original Goodow class</a>
 */
public abstract class BusProxy implements Bus {
    protected final Bus delegate;
    protected BusHook hook;

    public BusProxy(Bus delegate) {
        this.delegate = delegate;
    }

    public Bus getDelegate() {
        return delegate;
    }

    @Override
    public Bus setHook(BusHook hook) {
        this.hook = hook;
        return this;
    }

    @Override
    public Bus publish(String address, Object body, DeliveryOptions options) {
        return delegate.publish(address, body, options);
    }

    @Override
    public Bus send(String address, Object body, DeliveryOptions options) {
        return delegate.send(address, body, options);
    }

    @Override
    public <T> Bus request(String address, Object body, DeliveryOptions options, Handler<AsyncResult<Message<T>>> replyHandler) {
        return delegate.request(address, body, options, replyHandler);
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public <T> Registration register(String address, Handler<Message<T>> handler) {
        return delegate.register(address, handler);
    }

    @Override
    public <T> Registration registerLocal(String address, Handler<Message<T>> handler) {
        return delegate.registerLocal(address, handler);
    }

    @Override
    public <T> Registration register(boolean local, String address, Handler<Message<T>> handler) {
        return delegate.register(local, address, handler);
    }

    @Override
    public void close() {
        delegate.close();
    }
}