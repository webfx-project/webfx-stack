package dev.webfx.stack.session.state;

import java.util.function.Supplier;

/**
 * @author Bruno Salmon
 */
public final class SystemUserId {

    public static final SystemUserId SYSTEM = new SystemUserId("System");

    private final String name;

    public SystemUserId() {
        this("System");
    }

    public SystemUserId(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void run(Runnable runnable) {
        ThreadLocalStateHolder.runAsUser(this, runnable);
    }

    public <T> T callAndReturn(Supplier<T> supplier) {
        Object[] result = { null };
        ThreadLocalStateHolder.runAsUser(this, () -> result[0] = supplier.get());
        return (T) result[0];
    }

}
