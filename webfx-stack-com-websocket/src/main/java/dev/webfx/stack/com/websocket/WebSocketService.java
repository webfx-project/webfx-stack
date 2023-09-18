package dev.webfx.stack.com.websocket;

import dev.webfx.stack.com.websocket.spi.WebSocketServiceProvider;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class WebSocketService {

    public static WebSocketServiceProvider getProvider() {
        return SingleServiceProvider.getProvider(WebSocketServiceProvider.class, () -> ServiceLoader.load(WebSocketServiceProvider.class));
    }

    public static WebSocket createWebSocket(String url, ReadOnlyAstObject options) {
        return getProvider().createWebSocket(url, options);
    }

}
