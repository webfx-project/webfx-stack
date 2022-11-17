package dev.webfx.stack.session.state;

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

    public static Object getThreadLocalState() {
        return stateThreadLocal.get();
    }

}
