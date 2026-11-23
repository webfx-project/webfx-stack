package dev.webfx.stack.db.query.cli;

import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.scheduler.spi.SchedulerProvider;
import dev.webfx.platform.uischeduler.AnimationFramePass;
import dev.webfx.platform.uischeduler.spi.UiSchedulerProvider;

/**
 * The UI scheduler a terminal has, which is the ordinary one with the animation frames removed.
 *
 * <h3>Why this exists at all</h3>
 *
 * <p>{@code ClientSideStateSession} — which every client bus goes through, because {@code JsonClientBus}
 * calls the syncer on open, on close and on every message — schedules through {@code UiScheduler}. In a
 * browser or an OpenJFX app that provider comes from the UI kit. A CLI has no kit, and with no provider
 * {@code UiScheduler.getProvider()} throws on the first bus message.
 *
 * <h3>Why not the OpenJFX one, which already exists</h3>
 *
 * <p>Because it would be silent rather than wrong. {@code OpenJFXUiSchedulerProvider.scheduledImpl} sends a
 * deferred task to {@code WebFxKitLauncher.onReady(...)} while the kit is not ready, and a CLI never launches
 * a JavaFX application, so the kit is never ready and every deferred task is queued for a moment that does not
 * come. Its animation timer needs a started toolkit too. A probe whose scheduler quietly stops running tasks
 * is the failure shape this whole instrument exists to avoid, so the CLI brings a scheduler that runs things.
 *
 * <h3>What it does</h3>
 *
 * <p>Every plain scheduling method delegates to the platform's own {@link SchedulerProvider} — the JRE one in
 * a CLI — so timers, deferred tasks and background work behave exactly as they do anywhere else on the JRE.
 * The animation-frame methods have no frames to hang off, so they become plain scheduling: an "animation
 * frame" in a terminal is just "soon". Nothing in the bus, the session or the query path draws anything, so
 * no caller can tell the difference; a future caller that genuinely needs frames would need a real kit, not a
 * better stub here.
 *
 * <p>{@link #isUiThread()} is false, which is the honest answer and also the useful one: it makes
 * {@code ClientSideStateSession.callRunnable} run its work inline, which is the same path a browser app takes
 * during boot, before its UI has started.
 *
 * @author Claude Code
 */
public final class HeadlessUiSchedulerProvider implements UiSchedulerProvider {

    /**
     * Resolved once, and never through {@code UiScheduler}: that would come straight back here. The platform
     * provider for a CLI is {@code JreSchedulerProvider}, declared as a {@code SchedulerProvider} and not as a
     * {@code UiSchedulerProvider}, which is what keeps the two lookups distinct.
     */
    private final SchedulerProvider delegate;

    private int animationFrameNumber;

    public HeadlessUiSchedulerProvider() {
        SchedulerProvider platformProvider = Scheduler.getProvider();
        if (platformProvider == this)
            // Would be an infinite recursion on the first scheduled task. Can only happen if this class is
            // ever ALSO declared as a SchedulerProvider, which is a webfx.xml mistake rather than a runtime
            // condition - so it is worth failing on immediately and by name.
            throw new IllegalStateException(getClass().getSimpleName()
                + " is registered as the platform SchedulerProvider as well as the UiSchedulerProvider;"
                + " it must only be the second, or every scheduled task recurses");
        delegate = platformProvider;
        // Scheduler.runInUiThread() does NOTHING when no UI scheduler has been injected - no exception, no
        // fallback, the runnable is dropped. Injecting ourselves turns that silent drop into ordinary
        // deferred execution (isUiThread() is false, so the default runInUiThread defers).
        Scheduler.setUiSchedulerProvider(this);
    }

    // ---- The plain Scheduler API: the platform's, unchanged. ----

    @Override
    public Scheduled scheduleDeferred(Runnable runnable) {
        return delegate.scheduleDeferred(runnable);
    }

    @Override
    public Scheduled scheduleDelay(long delayMs, Runnable runnable) {
        return delegate.scheduleDelay(delayMs, runnable);
    }

    @Override
    public Scheduled schedulePeriodic(long delayMs, Runnable runnable) {
        return delegate.schedulePeriodic(delayMs, runnable);
    }

    @Override
    public Scheduled runInBackground(Runnable runnable) {
        return delegate.runInBackground(runnable);
    }

    @Override
    public Scheduled runOnIdle(Runnable runnable) {
        return delegate.runOnIdle(runnable);
    }

    @Override
    public int tasksCount(boolean includeDeferred, boolean includePeriodic, boolean includeBackground, boolean includePending, boolean includeRunning) {
        return delegate.tasksCount(includeDeferred, includePeriodic, includeBackground, includePending, includeRunning);
    }

    @Override
    public void wakeUp() {
        delegate.wakeUp();
    }

    /**
     * False, always. There is no UI thread in a terminal, and saying so is what makes the session code run
     * its listener calls inline instead of posting them to a thread that will never drain.
     */
    @Override
    public boolean isUiThread() {
        return false;
    }

    // ---- The animation-frame API: no frames, so "in a frame" means "soon". ----

    @Override
    public void requestNextScenePulse() {
        // Nothing paints, so there is no pulse to request.
    }

    @Override
    public boolean isAnimationFrameNow() {
        return false;
    }

    @Override
    public int getAnimationFrameNumber() {
        return animationFrameNumber;
    }

    @Override
    public Scheduled scheduleDelayInAnimationFrame(long delayMs, Runnable animationTask, int afterFrameCount, AnimationFramePass pass) {
        animationFrameNumber++;
        return delayMs <= 0 ? delegate.scheduleDeferred(animationTask) : delegate.scheduleDelay(delayMs, animationTask);
    }

    @Override
    public Scheduled schedulePeriodicInAnimationFrame(long delayMs, Runnable animationTask, AnimationFramePass pass) {
        // A period of 0 would spin a CLI's scheduler thread flat out. 16ms is the frame budget the base
        // provider assumes (1/60s), so a caller asking for "every frame" gets about what it asked for.
        return delegate.schedulePeriodic(delayMs <= 0 ? 16 : delayMs, () -> {
            animationFrameNumber++;
            animationTask.run();
        });
    }
}
