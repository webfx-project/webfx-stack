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
package dev.webfx.stack.com.bus.spi.impl.json.client.websocket;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.stack.com.bus.Bus;
import dev.webfx.stack.com.bus.BusHook;
import dev.webfx.stack.com.bus.spi.impl.client.BusHookProxy;
import dev.webfx.stack.com.websocket.WebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * @author 田传武 (aka Larry Tin) - author of Goodow realtime-channel project
 * @author Bruno Salmon - fork, refactor & update for the webfx project
 *
 * <a href="https://github.com/goodow/realtime-channel/blob/master/src/main/java/com/goodow/realtime/channel/impl/ReconnectBus.java">Original Goodow class</a>
 */
public final class ReconnectBus extends WebSocketBus {
    private static final String AUTO_RECONNECT = "reconnect";
    private BusHook hook;
    private boolean reconnect;
    private final List<String> queuedNetworkRawMessages = new ArrayList<>();
    private WebSocketBusOptions options;

    public ReconnectBus() {
    }

    @Override
    protected void onOptions(WebSocketBusOptions options) {
        super.onOptions(options);
        this.options = options;

        ReadOnlyAstObject socketOptions = options.getSocketOptions();
        reconnect = socketOptions == null || !socketOptions.has(AUTO_RECONNECT) || socketOptions.getBoolean(AUTO_RECONNECT);

        super.setHook(new BusHookProxy() {
            @Override
            public void handleOpened() {

                for (Map.Entry<String, Integer> entry : handlerCount.entrySet()) {
                    String address = entry.getKey();
                    if (entry.getValue() > 0) {
                        sendUnregister(address);
                        sendRegister(address);
                    }
                }
                handlerCount.clear();

                if (!queuedNetworkRawMessages.isEmpty()) {
                    List<String> copy = new ArrayList<>(queuedNetworkRawMessages);
                    queuedNetworkRawMessages.clear();
                    // Drain any messages that came in while the channel was not open.
                    for (String msg : copy) // copy.forEach doesn't compile with TeaVM
                        sendOutgoingNetworkRawMessage(msg);
                } else
                    sendPingState();
                super.handleOpened();
            }

            @Override
            public void handlePostClose() {
                if (reconnect) {
                    Scheduler.scheduleDelay(1000, () -> {
                        if (reconnect)
                            reconnect();
                    });
                }
                super.handlePostClose();
            }

            @Override
            protected BusHook delegate() {
                return hook;
            }
        });
    }

    @Override
    public Bus setHook(BusHook hook) {
        this.hook = hook;
        return this;
    }

    private void reconnect() {
        WebSocket.State readyState = getReadyState();
        if (readyState == WebSocket.State.OPEN || readyState == WebSocket.State.CONNECTING)
            return;
        if (webSocket != null)
            webSocket.close();
        connect(serverUri, options);
    }

    @Override
    protected void doClose() {
        reconnect = false;
        queuedNetworkRawMessages.clear();
        super.doClose();
    }

    @Override
    protected void sendOutgoingNetworkRawMessage(String rawMessage) {
        if (isOpen()) {
            super.sendOutgoingNetworkRawMessage(rawMessage);
            return;
        }
        if (reconnect)
            reconnect();
        ReadOnlyAstObject jsonRawMessage = parseJsonRawMessage(rawMessage);
        String type = jsonRawMessage.getString(TYPE);
        if ("ping".equals(type) || "register".equals(type))
            return;
        queuedNetworkRawMessages.add(rawMessage);
    }

    @Override
    protected boolean shouldClearReplyHandlerNow(String replyAddress) {
        // if it is a reply handler from a queued message, it shouldn't be cleared now because the message has not been
        // sent yet! It will be sent as soon as the bus will open and the reply handler should be called at the time
        // Note: using an old-fashion loop to avoid a ConcurrentModificationException
        for (int i = 0; i < queuedNetworkRawMessages.size(); i++) {
            String msg = queuedNetworkRawMessages.get(i);
            if (replyAddress.equals(parseJsonRawMessage(msg).getString(REPLY_ADDRESS)))
                return false;
        }
        return true;
        // previous code that was potentially raising ConcurrentModificationException
        //return Collections.noneMatch(queuedNetworkRawMessages, msg -> replyAddress.equals(parseJsonRawMessage(msg).getString(REPLY_ADDRESS)));
    }
}
