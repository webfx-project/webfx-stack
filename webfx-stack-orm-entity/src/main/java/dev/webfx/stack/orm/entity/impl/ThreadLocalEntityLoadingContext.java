package dev.webfx.stack.orm.entity.impl;

/**
 * Usage:
 *
 * try (var context = ThreadLocalEntityLoadingContext.open(true)) {
 *      ...
 *      any call to DynamicEntity.setFieldValue() won't record this as modification in an UpdateStore
 *      ...
 * }
 *
 * @author Bruno Salmon
 */
public final class ThreadLocalEntityLoadingContext implements AutoCloseable {

    private static final ThreadLocal<Boolean> entityLoadingThreadLocal = new ThreadLocal<>();

    private final Boolean previousEntityLoading = entityLoadingThreadLocal.get();

    private ThreadLocalEntityLoadingContext(Boolean entityLoading) {
        entityLoadingThreadLocal.set(entityLoading);
    }

    @Override
    public void close() {
        entityLoadingThreadLocal.set(previousEntityLoading);
    }

    public static ThreadLocalEntityLoadingContext open(Boolean entityLoading) {
        return entityLoading == null ? null : new ThreadLocalEntityLoadingContext(entityLoading);
    }

    public static boolean isThreadLocalEntityLoading() {
        return Boolean.TRUE.equals(entityLoadingThreadLocal.get());
    }
}
