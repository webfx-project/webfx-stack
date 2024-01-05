package dev.webfx.stack.orm.reactive.call;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.cache.CacheEntry;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.async.AsyncFunction;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Bruno Salmon
 */
public class ReactiveCall<A, R> {

    private final AsyncFunction<A, R> asyncFunction;
    private boolean fireCallWhenReadyRequested;
    private A lastCallArgument;
    private Supplier<A> argumentFetcher;
    private boolean fetchingArgument;
    private CacheEntry<R> resultCacheEntry;

    private final BooleanProperty startedProperty = new SimpleBooleanProperty() {
        @Override
        protected void invalidated() {
            if (get())
                onStarted();
            else
                onStopped();
        }
    };

    private final BooleanProperty activeProperty = new SimpleBooleanProperty(true) {
        @Override
        protected void invalidated() {
            onActiveChanged();
        }
    };

    private final ObjectProperty<A> argumentProperty = new SimpleObjectProperty<A/*GWT*/>() {
        @Override
        protected void invalidated() {
            onArgumentChanged();
        }
    };

    protected final ObjectProperty<R> resultProperty = new SimpleObjectProperty<R/*GWT*/>() {
        @Override
        protected void invalidated() {
            onResultChanged();
        }
    };

    public ReactiveCall(AsyncFunction<A, R> asyncFunction) {
        this.asyncFunction = asyncFunction;
    }

    public void setResultCacheEntry(CacheEntry<R> resultCacheEntry) {
        this.resultCacheEntry = resultCacheEntry;
        if (resultCacheEntry != null && isStarted()) {
            R result = getResult();
            if (result != null)
                resultCacheEntry.putValue(result);
            else
                initResultFromCacheIfApplicable();
        }
    }

    public final BooleanProperty activeProperty() {
        return activeProperty;
    }

    public final boolean isActive() {
        return activeProperty.get();
    }

    public final void setActive(boolean active) {
        activeProperty.setValue(active);
    }

    public final void bindActivePropertyTo(ObservableValue<Boolean> activeProperty) {
        if (activeProperty != null)
            this.activeProperty.bind(activeProperty);
    }

    public final ObjectProperty<A> argumentProperty() {
        return argumentProperty;
    }

    public final A getArgument() {
        return argumentProperty.get();
    }

    public final void setArgument(A argument) {
        argumentProperty.set(argument);
    }

    protected final BooleanProperty callingProperty = new SimpleBooleanProperty();

    public final ObservableBooleanValue callingProperty() {
        return callingProperty;
    }

    public final boolean isCalling() {
        return callingProperty.get();
    }

    protected final void setCalling(boolean calling) {
        callingProperty.setValue(calling);
    }

    public final ObservableValue<R> resultProperty() {
        return resultProperty;
    }

    public final R getResult() {
        return resultProperty.get();
    }

    protected final void setResult(R result) {
        resultProperty.setValue(result);
    }

    protected final ObjectProperty<Throwable> callExceptionProperty = new SimpleObjectProperty<>();

    public final Throwable getCallException() {
        return callExceptionProperty.get();
    }

    public final ObservableValue<Throwable> callExceptionProperty() {
        return callExceptionProperty;
    }

    private void setCallException(Throwable callException) {
        callExceptionProperty.set(callException);
    }

    public Supplier<A> getArgumentFetcher() {
        return argumentFetcher;
    }

    public void setArgumentFetcher(Supplier<A> argumentFetcher) {
        this.argumentFetcher = argumentFetcher;
    }

    private boolean fireCallNowIfRequiredScheduled;

    protected void scheduleFireCallNowIfRequired() {
        if (!fireCallNowIfRequiredScheduled) {
            fireCallNowIfRequiredScheduled = true;
            UiScheduler.scheduleDeferred(this::fireCallNowIfRequired);
        }
    }

    private void fireCallNowIfRequired() {
        fireCallNowIfRequiredScheduled = false;
        if (argumentFetcher != null) {
            fetchingArgument = true;
            A argument = argumentFetcher.get();
            if (argument != null)
                setArgument(argument);
            fetchingArgument = false;
        }
        // Recent changes => does that work with ReactiveQueryPushCall?
        if (getArgument() == null) {
            memorizeLastCallArgument();
            setResult(null);
            return;
        }
        if (isFireCallRequiredNow())
            fireCall();
    }

    protected boolean isFireCallRequiredNow() {
        return isReady() && evaluateConditionsToFireCallOnceReady();
    }

    protected boolean isReady() {
        return isStarted() && isActive();
    }

    protected boolean evaluateConditionsToFireCallOnceReady() {
        return fireCallWhenReadyRequested || hasArgumentChangedSinceLastCall();
    }

    protected void resetStateBeforeCallingAsyncFunction() {
        fireCallWhenReadyRequested = false;
    }

    private void onActiveChanged() {
        scheduleFireCallNowIfRequired();
    }

    public void onArgumentChanged() {
        if (!fetchingArgument)
            scheduleFireCallNowIfRequired();
        initResultFromCacheIfApplicable();
    }

    protected void onResultChanged() {
        if (resultCacheEntry != null && getArgument() != null)
            resultCacheEntry.putValue(getResult());
    }

    private void initResultFromCacheIfApplicable() {
        if (resultCacheEntry != null && getResult() == null && getArgument() != null)
            setResult(resultCacheEntry.getValue());
    }

    public void refreshWhenReady(boolean force) {
        if (force)
            fireCallWhenReadyRequested = true;
        scheduleFireCallNowIfRequired();
    }

    // Should be protected but causes a Java compiler error with OpenJDK-14.0.1 so leave it public for now
    public void fireCallWhenReady() {
        fireCallWhenReadyRequested = true;
        scheduleFireCallNowIfRequired();
    }

    protected void fireCall() {
        //log("ReactiveCall.fireCall()");
        resetStateBeforeCallingAsyncFunction();
        setCalling(true);
        callAsyncFunction();
    }

    protected void memorizeLastCallArgument() {
        lastCallArgument = getArgument();
    }

    protected A getLastCallArgument() {
        return lastCallArgument;
    }

    public boolean hasArgumentChangedSinceLastCall() {
        return hasArgumentChangedSinceLastCall(getArgument());
    }

    public boolean hasArgumentChangedSinceLastCall(A argument) {
        return !Objects.equals(argument, getLastCallArgument());
    }

    protected void callAsyncFunction() {
        memorizeLastCallArgument();
        asyncFunction.apply(getArgument()).onComplete(ar -> onCallResult(ar.result(), ar.cause()));
    }

    protected void onCallResult(R result, Throwable error) {
        // Double-checking if the argument is still the latest
        if (hasArgumentChangedSinceLastCall())
            log("Ignoring a received result coming from an old call");
        else {
            setCallException(error);
            if (error == null) {
                //log("ReactiveCall.onCallResult()");
                setResult(result);
            }
            setCalling(false);
        }
    }

    public BooleanProperty startedProperty() {
        return startedProperty;
    }

    public boolean isStarted() {
        return startedProperty.get();
    }

    public void setStarted(boolean active) {
        startedProperty.setValue(active);
    }

    public void start() {
        setStarted(true);
    }

    public void stop() {
        setStarted(false);
    }

    protected void onStarted() {
        scheduleFireCallNowIfRequired();
        rescheduleAutoRefresh();
        initResultFromCacheIfApplicable();
    }

    protected void onStopped() {
        stopAutoRefresh();
    }

    private final LongProperty autoRefreshDelayProperty = new SimpleLongProperty(0) {
        @Override
        protected void invalidated() {
            rescheduleAutoRefresh();
        }
    };

    public long getAutoRefreshDelay() {
        return autoRefreshDelayProperty.get();
    }

    public LongProperty autoRefreshDelayProperty() {
        return autoRefreshDelayProperty;
    }

    public void setAutoRefreshDelay(long autoRefreshDelay) {
        this.autoRefreshDelayProperty.set(autoRefreshDelay);
    }

    private Scheduled autoRefreshScheduled;
    private void rescheduleAutoRefresh() {
        stopAutoRefresh();
        long autoRefreshDelay = getAutoRefreshDelay();
        if (autoRefreshDelay > 0 && isReady())
            autoRefreshScheduled = UiScheduler.schedulePeriodic(autoRefreshDelay, this::fireCallWhenReady);
    }

    private void stopAutoRefresh() {
        if (autoRefreshScheduled != null) {
            autoRefreshScheduled.cancel();
            autoRefreshScheduled = null;
        }
    }

    protected void log(String message) {
        Console.log(message);
    }
}
