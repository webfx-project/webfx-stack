package dev.webfx.stack.com.websocket.spi.impl.gwt;

import dev.webfx.stack.com.websocket.WebSocket;
import dev.webfx.stack.com.websocket.WebSocketListener;

/**
 * @author Bruno Salmon
 */
final class GwtWebSocket implements WebSocket {

    private final SockJS sockJS;

    public GwtWebSocket(SockJS sockJS) {
        this.sockJS = sockJS;
    }

    @Override
    public void send(String data) {
        sockJS.send(data);
    }

    @Override
    public void close() {
        sockJS.close();
    }

    @Override
    public State getReadyState() {
        Object jsState = sockJS.readyState();
        if (jsState == SockJS.OPEN())
            return State.OPEN;
        if (jsState == SockJS.CONNECTING())
            return State.CONNECTING;
        if (jsState == SockJS.CLOSING())
            return State.CLOSING;
        if (jsState == SockJS.CLOSED())
            return State.CLOSED;
        throw new IllegalStateException("SockJS.readyState");
    }

    @Override
    public void setListener(WebSocketListener listener) {
        sockJS.setOnOpen(e -> listener.onOpen());
        sockJS.setOnMessage(e -> listener.onMessage(e.data));
        sockJS.setOnClose(listener::onClose);
        sockJS.setOnError(e -> listener.onError(e.data));
    }

}
