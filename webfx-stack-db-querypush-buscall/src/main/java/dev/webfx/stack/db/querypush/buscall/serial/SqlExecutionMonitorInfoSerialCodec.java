package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.DbErrorMonitorInfo;
import dev.webfx.stack.db.querypush.InFlightQueryMonitorInfo;
import dev.webfx.stack.db.querypush.SqlAnalyzeResultInfo;
import dev.webfx.stack.db.querypush.SqlExecutionMonitorInfo;
import dev.webfx.stack.db.querypush.StatementMonitorInfo;

public final class SqlExecutionMonitorInfoSerialCodec extends SerialCodecBase<SqlExecutionMonitorInfo> {

    private static final String CODEC_ID = "SqlExecutionMonitorInfo";
    private static final String READ_KEY = "read";
    private static final String WRITE_KEY = "write";
    private static final String TOP_STATEMENTS_KEY = "topStatements";
    private static final String IN_FLIGHT_KEY = "inFlight";
    private static final String ANALYZE_RESULTS_KEY = "analyzeResults";
    private static final String RECENT_ERRORS_KEY = "recentErrors";

    public SqlExecutionMonitorInfoSerialCodec() {
        super(SqlExecutionMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(SqlExecutionMonitorInfo arg, AstObject serial) {
        encodeObject(serial, READ_KEY,            arg.getRead());
        encodeObject(serial, WRITE_KEY,           arg.getWrite());
        encodeArray( serial, TOP_STATEMENTS_KEY,  arg.getTopStatements());
        encodeArray( serial, IN_FLIGHT_KEY,       arg.getInFlight());
        encodeArray( serial, ANALYZE_RESULTS_KEY, arg.getAnalyzeResults());
        encodeArray( serial, RECENT_ERRORS_KEY,   arg.getRecentErrors());
    }

    @Override
    public SqlExecutionMonitorInfo decode(ReadOnlyAstObject serial) {
        return new SqlExecutionMonitorInfo(
            decodeObject(serial, READ_KEY),
            decodeObject(serial, WRITE_KEY),
            decodeArray( serial, TOP_STATEMENTS_KEY, StatementMonitorInfo.class),
            decodeArray( serial, IN_FLIGHT_KEY, InFlightQueryMonitorInfo.class),
            decodeArray( serial, ANALYZE_RESULTS_KEY, SqlAnalyzeResultInfo.class),
            decodeArray( serial, RECENT_ERRORS_KEY, DbErrorMonitorInfo.class));
    }
}
