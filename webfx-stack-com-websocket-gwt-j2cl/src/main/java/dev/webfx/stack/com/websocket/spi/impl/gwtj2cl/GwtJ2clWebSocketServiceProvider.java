package dev.webfx.stack.com.websocket.spi.impl.gwtj2cl;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.websocket.spi.WebSocketServiceProvider;
import elemental2.dom.DomGlobal;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

import java.util.Objects;

/**
 * @author Bruno Salmon
 */
public final class GwtJ2clWebSocketServiceProvider implements WebSocketServiceProvider {

    @Override
    public GwtJ2clWebSocket createWebSocket(String url, ReadOnlyAstObject options) {
        // Code for the case the "sockjs-quickstart.js" script was included in index.html
        JsPropertyMap<Object> w = Js.asPropertyMap(DomGlobal.window);
        SockJS sockJS = (SockJS) w.get("quickStartSockJS");
        if (sockJS != null) { // Yes the script was included, so a sockJS has already been started
            w.delete("quickStartSockJS"); // Used only once on first call
            if (options == null && Objects.equals(w.get("quickStartSockJSUrl"), url)) // Checking the parameters are the same
                return new GwtJ2clWebSocket(sockJS); // Yes! The connection is probably already established (we gained a few seconds!)
            sockJS.close(); // The started connection is not the requested one! We close it.
        }
        // Otherwise we create a brand new SockJS connection
        return new GwtJ2clWebSocket(new SockJS(url, null, options));
    };

}
