package dev.webfx.stack.session.state;

import java.util.function.Supplier;

/**
 * @author Bruno Salmon
 */
public final class ThreadLocalStateHolder implements AutoCloseable {

    private static final ThreadLocal<Object> stateThreadLocal = new ThreadLocal<>();

    private final Object previousState = stateThreadLocal.get();

    private ThreadLocalStateHolder(Object state) {
        stateThreadLocal.set(state);
    }

    @Override
    public void close() {
        stateThreadLocal.set(previousState);
    }

    public static ThreadLocalStateHolder open(Object state) {
        return state == null ? null : new ThreadLocalStateHolder(state);
    }

    public static void runWithState(Object state, Runnable runnable) {
        try (ThreadLocalStateHolder ignored = ThreadLocalStateHolder.open(state)) {
            runnable.run();
        }
    }

    public static <T> T runWithState(Object state, Supplier<T> supplier) {
        try (ThreadLocalStateHolder ignored = ThreadLocalStateHolder.open(state)) {
            return supplier.get();
        }
    }

    public static void runAsUser(Object userId, Runnable runnable) {
        runWithState(StateAccessor.setUserId(null, userId), runnable);
    }

    public static Object getThreadLocalState() {
        return stateThreadLocal.get();
    }

    public static String getRunId() {
        return StateAccessor.getRunId(getThreadLocalState());
    }

    public static String getServerSessionId() {
        return StateAccessor.getServerSessionId(getThreadLocalState());
    }

    public static Object getUserId() {
        return StateAccessor.getUserId(getThreadLocalState());
    }

}
