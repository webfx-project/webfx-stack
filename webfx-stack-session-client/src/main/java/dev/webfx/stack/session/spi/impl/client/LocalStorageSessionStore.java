package dev.webfx.stack.session.spi.impl.client;

import dev.webfx.platform.async.Future;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.ast.json.JsonObject;
import dev.webfx.platform.ast.json.ReadOnlyJsonArray;
import dev.webfx.platform.storage.LocalStorage;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionStore;

import java.util.Map;

/**
 * @author Bruno Salmon
 */
final class LocalStorageSessionStore implements SessionStore {

    private final static String ITEM_KEY_PREFIX = "Session-";

    @Override
    public Session createSession() {
        return new InMemorySession();
    }

    @Override
    public Future<Session> get(String id) {
        String sessionItem = LocalStorage.getItem(sessionIdToLocalStorageItemKey(id));
        if (sessionItem == null)
            return Future.failedFuture("No such session in this store");
        try {
            JsonObject jsonObject = Json.parseObject(sessionItem);
            InMemorySession session = new InMemorySession(id);
            ReadOnlyJsonArray keys = jsonObject.keys();
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.getElement(i);
                session.put(key, SerialCodecManager.decodeFromJson(jsonObject.get(key)));
            }
            return Future.succeededFuture(session);
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    @Override
    public Future<Boolean> delete(String id) {
        LocalStorage.removeItem(sessionIdToLocalStorageItemKey(id));
        return Future.succeededFuture(true);
    }

    @Override
    public Future<Boolean> put(Session session) {
        Map<String, Object> values = ((InMemorySession) session).values;
        JsonObject jsonObject = Json.createObject();
        for (Map.Entry<String, Object> entry : values.entrySet())
            jsonObject.set(entry.getKey(), SerialCodecManager.encodeToJson(entry.getValue()));
        String sessionItem = jsonObject.toJsonString();
        LocalStorage.setItem(sessionIdToLocalStorageItemKey(session.id()), sessionItem);
        return Future.succeededFuture(true);
    }

    @Override
    public Future<Boolean> clear() {
        LocalStorage.getKeys().forEachRemaining(key -> {
            String sessionId = localStorageItemKeyToSessionId(key);
            if (sessionId != null)
                LocalStorage.removeItem(key);
        });
        return Future.succeededFuture(true);
    }

    private static String sessionIdToLocalStorageItemKey(String sessionId) {
        return ITEM_KEY_PREFIX + sessionId;
    }

    private String localStorageItemKeyToSessionId(String key) {
        return key.startsWith(ITEM_KEY_PREFIX) ? key.substring(ITEM_KEY_PREFIX.length()) : null;
    }
}
