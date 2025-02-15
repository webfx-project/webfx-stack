package dev.webfx.stack.com.websocket.spi.impl.gwtj2cl.nativ;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.websocket.spi.WebSocketServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class GwtJ2clNativeWebSocketServiceProvider implements WebSocketServiceProvider {

    @Override
    public GwtJ2clNativeWebSocket createWebSocket(String url, ReadOnlyAstObject options) {
        // Otherwise we create a brand new SockJS connection
        return new GwtJ2clNativeWebSocket(new elemental2.dom.WebSocket(url));
    };

}
