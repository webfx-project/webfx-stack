package dev.webfx.stack.framework.client.orm.reactive.call.query;

import dev.webfx.stack.framework.client.orm.reactive.call.ReactiveCall;
import dev.webfx.stack.platform.shared.services.query.QueryArgument;
import dev.webfx.stack.platform.shared.services.query.QueryResult;
import dev.webfx.stack.platform.shared.services.query.QueryService;
import dev.webfx.stack.platform.async.AsyncFunction;

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
