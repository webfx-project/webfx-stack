package dev.webfx.stack.session.isolation;

import dev.webfx.stack.session.Session;
import dev.webfx.stack.session.SessionService;
import dev.webfx.stack.session.SessionStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Bruno Salmon
 */
public final class ConversationRegistry {

    private final ConcurrentMap<String, IsolatedSession> isolatedSessions = new ConcurrentHashMap<>();

    public IsolatedSession getOrCreateIsolatedSession(String conversationId, long timeout) {
        return getOrCreateIsolatedSession(conversationId, timeout, SessionService.getSessionStore());
    }

    public IsolatedSession getOrCreateIsolatedSession(String conversationId, long timeout, SessionStore sessionStore) {
        return getOrCreateIsolatedSession(conversationId, sessionStore.createSession(timeout));
    }

    public IsolatedSession getOrCreateIsolatedSession(String conversationId, Session session) {
        return isolatedSessions.computeIfAbsent(conversationId, k -> new IsolatedSession(session, conversationId));
    }

    public IsolatedSession getIsolatedSession(String conversationId) {
        return isolatedSessions.get(conversationId);
    }

    public void removeIsolatedSession(IsolatedSession isolatedSession) {
        removeIsolatedSession(isolatedSession.getConversationId());
    }

    public void removeIsolatedSession(String conversationId) {
        isolatedSessions.remove(conversationId);
    }

}
