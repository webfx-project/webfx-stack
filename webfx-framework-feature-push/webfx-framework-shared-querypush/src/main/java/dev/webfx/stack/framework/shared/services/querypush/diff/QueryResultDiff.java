package dev.webfx.stack.framework.shared.services.querypush.diff;

import dev.webfx.stack.platform.shared.services.query.QueryResult;

/**
 * @author Bruno Salmon
 */
public interface QueryResultDiff {

    int getPreviousQueryResultVersionNumber();

    int getFinalQueryResultVersionNumber();

    QueryResult applyTo(QueryResult queryResult);

}
