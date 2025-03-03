package dev.webfx.stack.db.querysubmit;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.console.Console;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * @author Bruno Salmon
 */
final class ExecutionQueue {

    private record WaitingRequest<A, R>(A argument, Promise<R> promise, AsyncFunction<A, R> executor) {}

    private final String name;
    private final int executingQueueMaxSize;
    private final Queue<WaitingRequest<?, ?>> waitingRequests = new ArrayDeque<>();
    private final List<Object> executingRequests = new ArrayList<>();

    ExecutionQueue(String name, int executingQueueMaxSize) {
        this.name = name;
        this.executingQueueMaxSize = executingQueueMaxSize;
    }

    <A, R> Future<R> executeRequest(A argument, AsyncFunction<A, R> executor) {
        // Can it be executed now?
        if (executingRequests.size() < executingQueueMaxSize) { // Yes
            return executeRequestNow(argument, executor);
        }
        // No, so we put it in the waiting queue with a promise
        Promise<R> promise = Promise.promise();
        waitingRequests.add(new WaitingRequest<>(argument, promise, executor));
        return promise.future();
    }

    private <A, R> Future<R> executeRequestNow(A argument, AsyncFunction<A, R> executor) {
        executingRequests.add(argument);
        return executor.apply(argument)
            .onComplete(ar -> {
                executingRequests.remove(argument);
                executeNext();
            });
    }

    private void executeNext() {
        if (executingRequests.size() < executingQueueMaxSize && !waitingRequests.isEmpty()) {
            WaitingRequest waitingRequest = waitingRequests.poll();
            executeRequestNow(waitingRequest.argument, waitingRequest.executor)
                .onComplete(waitingRequest.promise);
        }
    }

    void log(String message) {
        Console.log("[" + name + " | " + waitingRequests.size() + " | " + executingRequests.size() + " ] " + message);
    }
}
