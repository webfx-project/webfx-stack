package dev.webfx.stack.com.websocket.spi.impl.gwtj2cl.nativ;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.com.websocket.WebSocket;
import dev.webfx.stack.com.websocket.WebSocketListener;
import elemental2.core.ArrayBuffer;
import elemental2.core.DataView;
import elemental2.dom.Blob;
import jsinterop.base.Js;

/**
 * @author Bruno Salmon
 */
final class GwtJ2clNativeWebSocket implements WebSocket {

    private final elemental2.dom.WebSocket nativeWebSocket;

    public GwtJ2clNativeWebSocket(elemental2.dom.WebSocket nativeWebSocket) {
        this.nativeWebSocket = nativeWebSocket;
    }

    @Override
    public void send(String data) {
        nativeWebSocket.send(data);
    }

    @Override
    public void close() {
        nativeWebSocket.close();
    }

    @Override
    public State getReadyState() {
        int sockJsState = nativeWebSocket.readyState;
        if (sockJsState == elemental2.dom.WebSocket.OPEN)
            return State.OPEN;
        if (sockJsState == elemental2.dom.WebSocket.CONNECTING)
            return State.CONNECTING;
        if (sockJsState == elemental2.dom.WebSocket.CLOSING)
            return State.CLOSING;
        if (sockJsState == elemental2.dom.WebSocket.CLOSED)
            return State.CLOSED;
        throw new IllegalStateException("Unrecognized readyState");
    }

    @Override
    public void setListener(WebSocketListener listener) {
        nativeWebSocket.onopen = e -> listener.onOpen();
        nativeWebSocket.onmessage = e -> {
            String dataType = Js.typeof(e.data);
            if ("String".equals(dataType))
                listener.onMessage(e.data.asString());
            else if (e.data instanceof Blob) {
                ((Blob) e.data).text().then(text -> {
                    listener.onMessage(text);
                    return null;
                });
            } else if (e.data instanceof ArrayBuffer) {
                ArrayBuffer arrayBuffer = (ArrayBuffer) e.data;
                // TODO: replace this String conversion with new TextDecoder("utf-8").decode(arrayBuffer) when upgrading to elemental 1.2.2
                byte[] bytes = new byte[arrayBuffer.byteLength];
                DataView dataView = new DataView(arrayBuffer);
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte) dataView.getInt8(i);
                }
                String text = new String(bytes);
                listener.onMessage(text);
            } else {
                Console.log("[GwtJ2clNativeWebSocket] ⚠️ Received message with unknown data type: " + dataType);
            }
        };
        nativeWebSocket.onclose = e -> listener.onClose(AST.createObject().set("code", e.code).set("reason", e.reason));
        nativeWebSocket.onerror = e -> listener.onError(e.toString());
    }

}
