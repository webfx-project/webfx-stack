package dev.webfx.stack.session.isolation;

import dev.webfx.stack.session.Session;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class IsolatedSession implements Session {

    private Session underlyingSession;
    private final String conversationId;
    private final Map<String, Object> isolatedValues = new HashMap<>();

    public IsolatedSession(Session underlyingSession, String conversationId) {
        this.underlyingSession = underlyingSession;
        this.conversationId = conversationId;
    }

    public Session getUnderlyingSession() {
        return underlyingSession;
    }

    public void setUnderlyingSession(Session underlyingSession) {
        this.underlyingSession = underlyingSession;
    }

    public String getConversationId() {
        return conversationId;
    }

    @Override
    public String id() {
        return underlyingSession.id();
    }

    @Override
    public Session put(String key, Object obj) {
        isolatedValues.put(key, obj);
        underlyingSession.put(key, obj);
        return this;
    }

    @Override
    public <T> T get(String key) {
        Object obj = isolatedValues.get(key);
        if (obj != null)
            return (T) obj;
        return underlyingSession.get(key);
    }

    @Override
    public <T> T remove(String key) {
        isolatedValues.remove(key);
        underlyingSession.remove(key);
        return null;
    }

    @Override
    public boolean isEmpty() {
        return isolatedValues.isEmpty() && underlyingSession.isEmpty();
    }

    @Override
    public long timeout() {
        return underlyingSession.timeout();
    }
}
