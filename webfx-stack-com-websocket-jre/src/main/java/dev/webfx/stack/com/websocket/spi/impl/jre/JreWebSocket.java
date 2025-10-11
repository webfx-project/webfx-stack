/*
 * Note: this code is a fork of Goodow realtime-android project https://github.com/goodow/realtime-android
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
package dev.webfx.stack.com.websocket.spi.impl.jre;

import dev.webfx.platform.ast.AstObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import dev.webfx.stack.com.websocket.WebSocket;
import dev.webfx.stack.com.websocket.WebSocketListener;
import dev.webfx.platform.ast.AST;
import dev.webfx.platform.console.Console;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

/*
 * @author Bruno Salmon
 */
public final class JreWebSocket implements WebSocket {
    private static final Charset charset = StandardCharsets.UTF_8;
    private static final CharsetDecoder decoder = charset.newDecoder();

    private static String toString(ByteBuffer buffer) throws CharacterCodingException {
        int old_position = buffer.position();
        String data = decoder.decode(buffer).toString();
        // reset buffer's position to its original so it is not altered:
        buffer.position(old_position);
        return data;
    }

    private final WebSocketClient socket;
    private WebSocketListener listener;

    public JreWebSocket(String uri) {
        URI serverUri;
        try {
            serverUri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        socket = new WebSocketClient(serverUri, new Draft_6455()) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                if (listener != null)
                    listener.onOpen();
            }

            @Override
            public void onMessage(String msg) {
                if (listener != null)
                    listener.onMessage(msg);
            }

            @Override
            public void onMessage(ByteBuffer buffer) {
                try {
                    if (listener != null)
                        listener.onMessage(JreWebSocket.toString(buffer));
                } catch (CharacterCodingException e) {
                    Console.log("Websocket Failed when Charset Decoding", e);
                }
            }

            @Override
            public void onError(Exception e) {
                if (listener != null) {
                    String message = e.getMessage();
                    listener.onError(message == null ? e.getClass().getSimpleName() : message);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (listener != null) {
                    AstObject object = AST.createObject();
                    object.set("code", code);
                    object.set("reason", reason);
                    object.set("remote", remote);
                    listener.onClose(object);
                }
            }
        };

        socket.connect();
    }

    @Override
    public State getReadyState() {
        switch (socket.getReadyState()) {
            case NOT_YET_CONNECTED: return State.CONNECTING;
            case OPEN: return State.OPEN;
            case CLOSED: return State.CLOSED;
            case CLOSING: return State.CLOSING;
        }
        return null;
    }

    @Override
    public void send(String data) {
        try {
            //Logger.log("WebSocket send: " + data);
            socket.getConnection().send(data);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setListener(WebSocketListener listener) {
        this.listener = listener;
    }


    @Override
    public void close() {
        socket.close();
    }
}