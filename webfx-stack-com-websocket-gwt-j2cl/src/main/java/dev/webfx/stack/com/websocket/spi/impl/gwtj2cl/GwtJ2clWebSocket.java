package dev.webfx.stack.com.websocket.spi.impl.gwtj2cl;

import dev.webfx.platform.ast.AST;
import dev.webfx.stack.com.websocket.WebSocket;
import dev.webfx.stack.com.websocket.WebSocketListener;

/**
 * @author Bruno Salmon
 */
final class GwtJ2clWebSocket implements WebSocket {

    private final SockJS sockJS;

    public GwtJ2clWebSocket(SockJS sockJS) {
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
        Object sockJsState = sockJS.readyState;
        if (sockJsState == SockJS.OPEN)
            return State.OPEN;
        if (sockJsState == SockJS.CONNECTING)
            return State.CONNECTING;
        if (sockJsState == SockJS.CLOSING)
            return State.CLOSING;
        if (sockJsState == SockJS.CLOSED)
            return State.CLOSED;
        throw new IllegalStateException("Unrecognized SockJS.readyState");
    }

    @Override
    public void setListener(WebSocketListener listener) {
        sockJS.onopen = e -> listener.onOpen();
        sockJS.onmessage = e -> listener.onMessage(e.data);
        sockJS.onclose = e -> listener.onClose(AST.createObject(e));
        sockJS.onerror = e -> listener.onError(e.data);
    }

}
