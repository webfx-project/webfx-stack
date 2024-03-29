package dev.webfx.stack.orm.reactive.call.query;

import dev.webfx.stack.orm.reactive.call.ReactiveCall;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryService;
import dev.webfx.platform.async.AsyncFunction;

/**
 * @author Bruno Salmon
 */
public class ReactiveQueryCall extends ReactiveCall<QueryArgument, QueryResult> {

    public ReactiveQueryCall() {
        this(QueryService::executeQuery);
    }

    public ReactiveQueryCall(AsyncFunction<QueryArgument, QueryResult> queryFunction) {
        super(queryFunction);
    }

}
