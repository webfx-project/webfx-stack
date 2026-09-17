package dev.webfx.stack.db.querypush.buscall;

/**
 * @author Bruno Salmon
 */
public final class QueryPushServiceBusAddress {
    public static final String EXECUTE_QUERY_PUSH_METHOD_ADDRESS = "service/querypush/executeQueryPush";
    public static final String GET_MONITOR_INFO_METHOD_ADDRESS = "service/querypush/getMonitorInfo";
    public static final String GET_DATABASE_HEALTH_INFO_METHOD_ADDRESS = "service/querypush/getDatabaseHealthInfo";
    public static final String CANCEL_SQL_QUERY_METHOD_ADDRESS = "service/querypush/cancelSqlQuery";
    public static final String ARM_SQL_ANALYZE_METHOD_ADDRESS = "service/querypush/armSqlAnalyze";
    public static final String GET_SQL_ANALYZE_RESULT_METHOD_ADDRESS = "service/querypush/getSqlAnalyzeResult";
    public static final String RESET_SQL_ANALYZE_METHOD_ADDRESS = "service/querypush/resetSqlAnalyze";
    public static final String RESET_SQL_MONITOR_METHOD_ADDRESS = "service/querypush/resetSqlMonitor";

}
