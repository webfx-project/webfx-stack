package dev.webfx.stack.framework.client.orm.reactive.dql.querypush;

import dev.webfx.stack.framework.client.orm.reactive.dql.statement.ReactiveDqlStatement;
import dev.webfx.stack.framework.client.orm.reactive.call.query.ReactiveQueryCall;
import dev.webfx.stack.framework.client.orm.reactive.call.query.push.ReactiveQueryOptionalPush;
import dev.webfx.stack.framework.client.orm.reactive.call.query.push.ReactiveQueryPushCall;
import dev.webfx.stack.framework.client.orm.reactive.dql.query.ReactiveDqlQuery;

/**
 * @author Bruno Salmon
 */
public final class ReactiveDqlOptionalQueryPush<E> extends ReactiveDqlQuery<E> {

    public ReactiveDqlOptionalQueryPush(ReactiveDqlStatement<E> reactiveDqlStatement) {
        super(reactiveDqlStatement, new ReactiveQueryOptionalPush(new ReactiveQueryCall(), new ReactiveQueryPushCall()));
    }
}
