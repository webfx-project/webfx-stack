package dev.webfx.stack.session.isolation;

import dev.webfx.stack.session.Session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Bruno Salmon
 */
public final class ConversationRegistry {

    private final ConcurrentMap<String, IsolatedSession> isolatedSessions = new ConcurrentHashMap<>();

    public IsolatedSession getOrCreateIsolatedSession(String conversationId, Session session) {
        return isolatedSessions.computeIfAbsent(conversationId, k -> new IsolatedSession(session, conversationId));
    }

    public IsolatedSession getIsolatedSession(String conversationId) {
        return isolatedSessions.get(conversationId);
    }

    public IsolatedSession removeIsolatedSession(IsolatedSession isolatedSession) {
        return removeIsolatedSession(isolatedSession.getConversationId());
    }

    public IsolatedSession removeIsolatedSession(String conversationId) {
        return isolatedSessions.remove(conversationId);
    }

    public int getIsolatedSessionsCount() {
        return isolatedSessions.size();
    }

}
