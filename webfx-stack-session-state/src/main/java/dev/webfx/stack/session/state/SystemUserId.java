package dev.webfx.stack.session.state;

/**
 * @author Bruno Salmon
 */
public final class SystemUserId {

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
}
