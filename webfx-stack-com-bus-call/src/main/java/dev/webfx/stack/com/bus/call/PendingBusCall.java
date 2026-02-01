package dev.webfx.stack.com.bus.call;

import dev.webfx.platform.async.AsyncResult;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Handler;
import dev.webfx.platform.async.Promise;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class PendingBusCall<T> {

    private static final List<PendingBusCall> PENDING_CALLS = new ArrayList<>();
    private static final List<Handler<Integer>> PENDING_CALLS_COUNT_HANDLERS = new ArrayList<>();

    public static void addPendingCallsCountHandler(Handler<Integer> pendingCallsCountHandler) {
        PENDING_CALLS_COUNT_HANDLERS.add(pendingCallsCountHandler);
    }

    public static int getPendingCallsCount() {
        return PENDING_CALLS.size();
    }

    private final Promise<T> promise = Promise.promise();
    PendingBusCall() {
        updatePendingCalls(true);
    }

    void onBusCallResult(AsyncResult<BusCallResult<T>> busCallAsyncResult) {
        // Getting the bus call result that holds the final target result to return to the initial caller
        BusCallResult<T> busCallResult = busCallAsyncResult.result();
        if (busCallResult == null && busCallAsyncResult.succeeded()) { // Presumably timeout, what else can it be? TODO investigate further
            promise.fail("No reply from server (timeout)");
        } else {
            // Getting the result of the bus call that needs to be returned to the initial caller
            Object result = busCallAsyncResult.succeeded() ? busCallResult.getTargetResult() : busCallAsyncResult.cause();
            // Does it come from an asynchronous operation? (which returns an AsyncResult instance)
            if (result instanceof AsyncResult) { // if yes
                AsyncResult<T> ar = (AsyncResult<T>) result;
                // What needs to be returned is the successful result (if succeeded) or the exception (if failed)
                result = ar.succeeded() ? ar.result() : ar.cause();
            }
            // Now the result object is either the successful result or the exception whatever the nature of the operation (asynchronous or synchronous)
            if (result instanceof Throwable) // if it is an exception
                promise.tryFail((Throwable) result); // we finally mark the pending call as failed and return that exception (if not already failed)
            else // otherwise it is as successful result
                promise.complete((T) result); // so we finally mark the pending call as complete and return that result (in the expected class result)
        }
        // Updating the pending calls property
        updatePendingCalls(false);
    }

    public Future<T> future() {
        return promise.future();
    }

    private void updatePendingCalls(boolean addition) {
        if (addition)
            PENDING_CALLS.add(this);
        else
            PENDING_CALLS.remove(this);
        int pendingCallsCount = PENDING_CALLS.size();
        PENDING_CALLS_COUNT_HANDLERS.forEach(h -> h.handle(pendingCallsCount));
    }
}
