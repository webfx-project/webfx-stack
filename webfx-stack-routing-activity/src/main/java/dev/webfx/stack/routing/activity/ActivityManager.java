package dev.webfx.stack.routing.activity;

import dev.webfx.stack.routing.activity.impl.ActivityContextBase;
import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Handler;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.util.function.Factory;

/**
 * @author Bruno Salmon
 */
public final class ActivityManager<C extends ActivityContext<C>> {

    enum State {LAUNCHED, CREATED, STARTED, RESUMED, PAUSED, STOPPED, DESTROYED}

    private final Factory<Activity<C>> activityFactory;
    private final ActivityContextFactory<C> contextFactory;
    private Activity<C> activity;
    private State currentState = State.LAUNCHED;
    private C context;

    private ActivityManager(Factory<Activity<C>> activityFactory, ActivityContextFactory<C> contextFactory) {
        this.activityFactory = activityFactory;
        this.contextFactory = contextFactory;
    }

    private ActivityManager(Activity<C> activity, ActivityContextFactory<C> contextFactory) {
        this.activityFactory = null;
        this.activity = activity;
        this.contextFactory = contextFactory;
    }

    public ActivityManager(Activity<C> activity, C context, ActivityContextFactory<C> contextFactory) {
        this(activity, contextFactory);
        init(context);
    }

    private ActivityManager(Activity<C> activity, C context) {
        this(activity, context == null ? null: context.getActivityContextFactory());
        init(context);
    }

    private void init(C context) {
        if (context != null)
            ActivityContextBase.toActivityContextBase(context).setActivityManager((ActivityManager) this);
        this.context = context;
    }

    public C getContext() {
        return context;
    }

    public ActivityContextFactory<C> getContextFactory() {
        return contextFactory;
    }

    public Activity<C> getActivity() {
        return activity;
    }

    public Future<Void> create(C context) {
        init(context);
        return create();
    }

    public Future<Void> create() {
        return transitTo(State.CREATED);
    }

    public Future<Void> start() {
        return transitTo(State.STARTED);
    }

    public Future<Void> run(C context) {
        create(context);
        return run();
    }

    public Future<Void> run() {
        return resume();
    }

    public Future<Void> resume() {
        return transitTo(State.RESUMED);
    }

    public Future<Void> pause() {
        return transitTo(State.PAUSED);
    }

    public Future<Void> stop() {
        return transitTo(State.STOPPED);
    }

    public Future<Void> restart() {
        return transitTo(State.STARTED);
    }

    public Future<Void> destroy() {
        return transitTo(State.DESTROYED);
    }

    public Future<Void> transitTo(State intentState) {
        return onNoPendingTransitTo(intentState, Promise.promise(), Promise.promise());
    }

    private Promise<Void> lastPendingPromise; // mirror of the last pending transition future - an internal handler can be set on it

    private Future<Void> onNoPendingTransitTo(State intentState, Promise<Void> transitPromise, Promise<Void> pendingPromise) {
        synchronized (this) {
            Promise<Void> waiting = lastPendingPromise;
            lastPendingPromise = pendingPromise;
            if (waiting == null)
                return transitTo(intentState, transitPromise, pendingPromise);
            // Waiting the last transition to finish before processing this one
            waiting.future()
                    .onSuccess(v -> transitTo(intentState, transitPromise, pendingPromise))
                    .onFailure(cause -> failPromises(cause, transitPromise, pendingPromise));
            return transitPromise.future();
        }
    }

    private Future<Void> transitTo(State intentState, Promise<Void> transitPromise, Promise<Void> pendingPromise) {
        synchronized (this) {
            if (intentState == currentState)
                return completePromises(transitPromise, pendingPromise);
            State nextState;
            if (intentState.compareTo(currentState) > 0)
                nextState = State.values()[currentState.ordinal() + 1];
            else if (currentState == State.PAUSED && intentState == State.RESUMED
                || currentState == State.STOPPED && intentState == State.STARTED)
                nextState = intentState;
            else
                return failPromises("Illegal state transition", transitPromise, pendingPromise);
            onStateChanged(nextState).onComplete(new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> result) {
                    if (result.failed())
                        failPromises(result.cause(), transitPromise, pendingPromise);
                    else if (intentState == currentState)
                        completePromises(transitPromise, pendingPromise);
                    else
                        transitTo(intentState, Promise.promise(), pendingPromise).onComplete(this);
                }
            });
        }
        return transitPromise.future();
    }

    private Future<Void> completePromises(Promise<Void> transitPromise, Promise<Void> pendingPromise) {
        transitPromise.complete();
        return syncPromises(transitPromise, pendingPromise);
    }

    private Future<Void> failPromises(String failureMessage, Promise<Void> transitPromise, Promise<Void> pendingPromise) {
        transitPromise.fail(failureMessage);
        return syncPromises(transitPromise, pendingPromise);
    }

    private Future<Void> failPromises(Throwable throwable, Promise<Void> transitPromise, Promise<Void> pendingPromise) {
        transitPromise.fail(throwable);
        return syncPromises(transitPromise, pendingPromise);
    }

    private Future<Void> syncPromises(Promise<Void> transitPromise, Promise<Void> pendingPromise) {
        if (transitPromise.future().isComplete()) {
            synchronized (this) {
                if (lastPendingPromise == pendingPromise)
                    lastPendingPromise = null;
            }
            if (!pendingPromise.future().isComplete())
                if (transitPromise.future().failed())
                    pendingPromise.fail(transitPromise.future().cause());
                else
                    pendingPromise.complete();
        }
        return transitPromise.future();
    }

    private Future<Void> onStateChanged(State newState) {
        switch (currentState = newState) {
            case CREATED:
                if (activity == null)
                    activity = activityFactory.create();
                return activity.onCreateAsync(context);
            case STARTED: return activity.onStartAsync();
            case RESUMED: return activity.onResumeAsync();
            case PAUSED: return activity.onPauseAsync();
            case STOPPED: return activity.onStopAsync();
            case DESTROYED: {
                Future<Void> future = activity.onDestroyAsync();
                if (future.succeeded())
                    activity = null;
                return future;
            }
        }
        return Future.failedFuture("Unknown state"); // Should never occur
    }

    public static <C extends ActivityContext> void runActivity(Activity<C> activity, C context) {
        new ActivityManager(activity, context).run();
    }

    public static <C extends ActivityContext<C>> Factory<ActivityManager<C>> factory(Factory<Activity<C>> activityFactory, ActivityContextFactory<C> contextFactory) {
        return () -> new ActivityManager<>(activityFactory, contextFactory);
    }
}
