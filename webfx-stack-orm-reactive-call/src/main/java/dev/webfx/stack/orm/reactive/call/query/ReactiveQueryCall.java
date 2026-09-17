package dev.webfx.stack.orm.reactive.call.query;

import dev.webfx.platform.async.AsyncFunction;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryService;
import dev.webfx.stack.orm.reactive.call.ReactiveCall;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Bruno Salmon
 */
public class ReactiveQueryCall extends ReactiveCall<QueryArgument, QueryResult> {

    // Per-JVM counter — each ReactiveQueryCall instance gets a unique callId. The server combines
    // it with the client's runId to derive a globally-unique key for AsyncQueue source coalescing.
    private static final AtomicInteger NEXT_CALL_ID = new AtomicInteger();

    private final int callId = NEXT_CALL_ID.incrementAndGet();
    // Bumped on every decorateArgument() invocation; the server echoes it back into
    // QueryResult.callSeq, and onCallResult drops anything whose seq doesn't match.
    private int nextCallSeq;

    public ReactiveQueryCall() {
        this(QueryService::executeQuery);
    }

    public ReactiveQueryCall(AsyncFunction<QueryArgument, QueryResult> queryFunction) {
        super(queryFunction);
    }

    public int getCallId() {
        return callId;
    }

    @Override
    protected QueryArgument decorateArgument(QueryArgument argument) {
        if (argument == null)
            return null;
        // Caller-set values always win — we only fill in defaults from this ReactiveQueryCall.
        boolean needsCallId = argument.getCallId() == 0;
        boolean needsPriority = argument.getPriority() == QueryArgument.STANDARD_PRIORITY
                                && getPriority() != QueryArgument.STANDARD_PRIORITY;
        // Always stamp a fresh seq, even when other metadata is already present — the seq is
        // per-fire and must change on every invocation for the server-echo filter to work.
        return QueryArgument.builder()
            .copy(argument)
            .setCallId(needsCallId ? callId : argument.getCallId())
            .setPriority(needsPriority ? getPriority() : argument.getPriority())
            .setCallSeq(++nextCallSeq)
            .build();
    }

    @Override
    protected void onCallResult(QueryResult result, Throwable error) {
        // A result for an earlier fire arrived after we already sent a newer one — discard so the
        // out-of-order arrival doesn't overwrite the latest result.
        if (result != null && result.getCallSeq() > 0 && result.getCallSeq() != nextCallSeq) {
            log("Ignoring stale QueryResult (callSeq=" + result.getCallSeq() + ", expected=" + nextCallSeq + ")");
            return;
        }
        super.onCallResult(result, error);
    }
}
