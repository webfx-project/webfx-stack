package dev.webfx.stack.framework.shared.services.querypush.diff;

import dev.webfx.stack.db.query.QueryResult;

/**
 * @author Bruno Salmon
 */
public interface QueryResultDiff {

    int getPreviousQueryResultVersionNumber();

    int getFinalQueryResultVersionNumber();

    QueryResult applyTo(QueryResult queryResult);

}
