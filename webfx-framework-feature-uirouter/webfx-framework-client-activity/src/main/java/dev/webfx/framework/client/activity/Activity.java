package dev.webfx.framework.client.activity;

import dev.webfx.platform.shared.async.AsyncUtil;
import dev.webfx.platform.shared.async.Future;

/**
 * @author Bruno Salmon
 */
public interface Activity<C extends ActivityContext> {

    /** Async API **/

    default Future<Void> onCreateAsync(C context) { return AsyncUtil.consumeAsync(this::onCreate, context); }

    default Future<Void> onStartAsync() { return AsyncUtil.runAsync(this::onStart); }

    default Future<Void> onResumeAsync() { return AsyncUtil.runAsync(this::onResume); }

    default Future<Void> onPauseAsync() { return AsyncUtil.runAsync(this::onPause); }

    default Future<Void> onStopAsync() { return AsyncUtil.runAsync(this::onStop); }

    default Future<Void> onDestroyAsync() { return AsyncUtil.runAsync(this::onDestroy); }


    /** Sync API **/

    default void onCreate(C context) {}

    default void onStart() {}

    default void onResume() {}

    default void onPause() {}

    default void onStop() {}

    default void onDestroy() {}
}
