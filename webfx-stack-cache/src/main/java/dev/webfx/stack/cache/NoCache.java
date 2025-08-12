package dev.webfx.stack.cache;

/**
 * @author Bruno Salmon
 */
public final class NoCache implements Cache {

    @Override
    public void put(String key, Object value) {
    }

    @Override
    public Object get(String key) {
        return null;
    }
}
