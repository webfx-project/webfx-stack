package dev.webfx.stack.querypush.diff.impl;

import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryResultBuilder;
import dev.webfx.stack.querypush.diff.QueryResultComparator;
import dev.webfx.stack.querypush.diff.QueryResultDiff;

/**
 * @author Bruno Salmon
 */
public final class QueryResultTranslation implements QueryResultDiff {

    private final QueryResult rowsBefore;
    private final int rowStart;
    private final int rowEnd;
    private final QueryResult rowsAfter;
    private final int previousQueryResultVersionNumber;
    private final int finalQueryResultVersionNumber;

    public QueryResultTranslation(QueryResult rowsBefore, int rowStart, int rowEnd, QueryResult rowsAfter, int previousQueryResultVersionNumber, int finalQueryResultVersionNumber) {
        this.rowsBefore = rowsBefore;
        this.rowStart = rowStart;
        this.rowEnd = rowEnd;
        this.rowsAfter = rowsAfter;
        this.previousQueryResultVersionNumber = previousQueryResultVersionNumber;
        this.finalQueryResultVersionNumber = finalQueryResultVersionNumber;
    }

    public QueryResult getRowsBefore() {
        return rowsBefore;
    }

    public int getRowStart() {
        return rowStart;
    }

    public int getRowEnd() {
        return rowEnd;
    }

    public QueryResult getRowsAfter() {
        return rowsAfter;
    }

    @Override
    public int getPreviousQueryResultVersionNumber() {
        return previousQueryResultVersionNumber;
    }

    @Override
    public int getFinalQueryResultVersionNumber() {
        return finalQueryResultVersionNumber;
    }

    @Override
    public QueryResult applyTo(QueryResult queryResult) {
        int beforeCount = rowsBefore == null ? 0 : rowsBefore.getRowCount();
        int translationCount = rowEnd - rowStart + 1;
        int afterCount = rowsAfter == null ? 0 : rowsAfter.getRowCount();
        int rowCount = beforeCount + translationCount + afterCount;
        int columnCount = queryResult.getColumnCount();
        QueryResultBuilder rsb = QueryResultBuilder.create(rowCount, columnCount);
        QueryResultComparator.copyRows(rowsBefore, 0, beforeCount - 1, rsb, 0);
        QueryResultComparator.copyRows(queryResult, rowStart, rowEnd, rsb, beforeCount);
        QueryResultComparator.copyRows(rowsAfter, 0, afterCount - 1, rsb, beforeCount + translationCount);
        QueryResult rs = rsb.build();
        rs.setVersionNumber(finalQueryResultVersionNumber);
        return rs;
    }

}
