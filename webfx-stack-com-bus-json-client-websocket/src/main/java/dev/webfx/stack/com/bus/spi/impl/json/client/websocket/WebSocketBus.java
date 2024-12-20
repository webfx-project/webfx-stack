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
package dev.webfx.stack.com.bus.spi.impl.json.client.websocket;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.util.Booleans;
import dev.webfx.stack.com.bus.spi.impl.json.client.JsonClientBus;
import dev.webfx.stack.com.websocket.WebSocket;
import dev.webfx.stack.com.websocket.WebSocketListener;
import dev.webfx.stack.com.websocket.WebSocketService;

/*
 * @author 田传武 (aka Larry Tin) - author of Goodow realtime-channel project
 * @author Bruno Salmon - fork, refactor & update for the webfx project
 *
 * <a href="https://github.com/goodow/realtime-channel/blob/master/src/main/java/com/goodow/realtime/channel/impl/WebSocketBus.java">Original Goodow class</a>
 */
@SuppressWarnings("rawtypes")
public class WebSocketBus extends JsonClientBus {

    // Can be set to true when debugging Scheduler for other purposes than this class (reduces Scheduler sollicitations)
    private static final boolean SKIP_SCHEDULER_DEBUG_FLAG = false;

    private WebSocketListener internalWebSocketHandler;
    String serverUri;
    WebSocket webSocket;
    private int pingInterval;
    private Scheduled pingScheduled;
    // Possible external web socket listener to observe web socket connection state
    private WebSocketListener webSocketListener;

    WebSocketBus() {
        super(false);
        ConfigLoader.onConfigLoaded("webfx.stack.com.client.websocket", config -> {
            WebSocketBusOptions options = new WebSocketBusOptions();
            options.applyConfig(config);
            //options.turnUnsetPropertiesToDefault(); // should be already done by the platform but just in case
            onOptions(options);
        });
    }

    protected void onOptions(WebSocketBusOptions options) {
        String serverUri =
                (options.getProtocol() == WebSocketBusOptions.Protocol.WS ? "ws" : "http")
                        + (Booleans.isTrue(options.isServerSSL()) ? "s://" : "://")
                        + options.getServerHost()
                        + ':' + options.getServerPort()
                        + '/' + options.getBusPrefix()
                        + (options.getProtocol() == WebSocketBusOptions.Protocol.WS ? "/websocket" : "");
        internalWebSocketHandler = new WebSocketListener() {
            @Override
            public void onOpen() {
                publishOnOpenEvent();
                if (webSocketListener != null)
                    webSocketListener.onOpen();
            }

            @Override
            public void onMessage(String msg) {
                WebSocketBus.this.onIncomingNetworkRawMessage(msg);
                if (webSocketListener != null)
                    webSocketListener.onMessage(msg);
                // When running in the browser, the Scheduler can lack of reactivity when the tab is hidden, but not the
                // websocket communication, so it's a good opportunity to wake it up and ensure that all pending
                // operations are processed. It's especially important when receiving a login message from the magic
                // link app (which can be open in a separate tab in the same browser, making this app hidden) to ensure
                // we send back the acknowledgement, so the magic link app can confirm the user it reached out this app
                // with a successful login.
                if (!SKIP_SCHEDULER_DEBUG_FLAG)
                    Scheduler.wakeUp();
            }

            @Override
            public void onError(String error) {
                publishOnError(AST.createObject().set("message", error));
                if (webSocketListener != null)
                    webSocketListener.onError(error);
            }

            @Override
            public void onClose(ReadOnlyAstObject reason) {
                publishOnCloseEvent(reason);
                if (webSocketListener != null)
                    webSocketListener.onClose(reason);
            }
        };

        connect(serverUri, options);
    }

    public void setWebSocketListener(WebSocketListener webSocketListener) {
        this.webSocketListener = webSocketListener;
    }

    void connect(String serverUri, WebSocketBusOptions options) {
        this.serverUri = serverUri;
        pingInterval = options.getPingInterval();

        if (webSocketListener == null)
            Console.log("Connecting bus to " + serverUri);

        webSocket = WebSocketService.createWebSocket(serverUri, options.getSocketOptions());
        webSocket.setListener(internalWebSocketHandler);
    }

    WebSocket.State getReadyState() {
        return webSocket.getReadyState();
    }

    @Override
    public boolean isOpen() {
        return getReadyState() == WebSocket.State.OPEN;
    }

    @Override
    protected void doClose() {
        webSocket.close();
    }

    @Override
    protected void onOpen() {
        scheduleNextPing();
        super.onOpen();
    }

    @Override
    protected void onClose(Object reason) {
        super.onClose(reason);
        cancelPingTimer();
    }

    @Override
    protected void onIncomingNetworkRawMessage(String rawMessage) {
        touchNetworkMessageReceived();
        super.onIncomingNetworkRawMessage(rawMessage);
    }

    @Override
    protected void sendOutgoingNetworkRawMessage(String rawMessage) {
        if (!isOpen())
            throw new IllegalStateException("INVALID_STATE_ERR");
        logRawMessage(rawMessage, false);
        webSocket.send(rawMessage);
        notifyOutgoingTraffic();
    }

    protected void sendPing() {
        super.sendPing();
        scheduleNextPing(); // in order to schedule the next ping
    }

    private void scheduleNextPing() {
        cancelPingTimer();
        if (!SKIP_SCHEDULER_DEBUG_FLAG)
            pingScheduled = Scheduler.scheduleDelay(pingInterval, this::sendPing);
    }

    private void cancelPingTimer() {
        if (pingScheduled != null)
            pingScheduled.cancel();
        pingScheduled = null;
    }

    private void touchNetworkMessageReceived() {
        // Since we just received a message, we are sure the bus is connected so we can reschedule the next ping for
        // another new ping interval. In this way we can reduce the bus traffic (especially when there are lots of
        // clients). And if there is a server ping (such as the one provided by the push server service), this can
        // actually completely remove the client ping traffic.
        // FINALLY COMMENTED BECAUSE VERT.X (EVENT BUS BRIDGE) REQUIRES PING EVEN WHILE WEB SOCKET TRAFFIC
        // scheduleNextPing();
    }
}