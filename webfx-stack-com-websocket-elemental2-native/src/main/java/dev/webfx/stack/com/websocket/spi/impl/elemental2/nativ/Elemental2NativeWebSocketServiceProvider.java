package dev.webfx.stack.com.websocket.spi.impl.elemental2.nativ;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.websocket.spi.WebSocketServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class Elemental2NativeWebSocketServiceProvider implements WebSocketServiceProvider {

    @Override
    public Elemental2NativeWebSocket createWebSocket(String url, ReadOnlyAstObject options) {
        // Otherwise we create a brand new SockJS connection
        return new Elemental2NativeWebSocket(new elemental2.dom.WebSocket(url));
    };

}
