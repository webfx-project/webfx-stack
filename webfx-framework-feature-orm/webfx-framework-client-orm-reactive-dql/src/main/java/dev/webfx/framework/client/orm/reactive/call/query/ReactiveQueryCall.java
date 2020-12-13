package dev.webfx.framework.client.orm.reactive.call.query;

import dev.webfx.framework.client.orm.reactive.call.ReactiveCall;
import dev.webfx.platform.shared.services.query.QueryArgument;
import dev.webfx.platform.shared.services.query.QueryResult;
import dev.webfx.platform.shared.services.query.QueryService;
import dev.webfx.platform.shared.util.async.AsyncFunction;

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
