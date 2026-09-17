package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.SqlKindMonitorInfo;

public final class SqlKindMonitorInfoSerialCodec extends SerialCodecBase<SqlKindMonitorInfo> {

    private static final String CODEC_ID = "SqlKindMonitorInfo";
    private static final String COUNT_KEY = "count";
    private static final String TOTAL_NANOS_KEY = "totalNanos";
    private static final String ERROR_COUNT_KEY = "errorCount";
    private static final String WAITING_KEY = "waiting";
    private static final String EXECUTING_KEY = "executing";
    private static final String MAX_CONCURRENCY_KEY = "maxConcurrency";
    private static final String PEAK_WAITING_KEY = "peakWaiting";
    private static final String SHED_KEY = "shed";

    public SqlKindMonitorInfoSerialCodec() {
        super(SqlKindMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(SqlKindMonitorInfo arg, AstObject serial) {
        encodeLong(   serial, COUNT_KEY,           arg.getCount());
        encodeLong(   serial, TOTAL_NANOS_KEY,     arg.getTotalNanos());
        encodeLong(   serial, ERROR_COUNT_KEY,     arg.getErrorCount());
        encodeInteger(serial, WAITING_KEY,         arg.getWaiting());
        encodeInteger(serial, EXECUTING_KEY,       arg.getExecuting());
        encodeInteger(serial, MAX_CONCURRENCY_KEY, arg.getMaxConcurrency());
        encodeInteger(serial, PEAK_WAITING_KEY,    arg.getPeakWaiting());
        encodeInteger(serial, SHED_KEY,            arg.getShed());
    }

    @Override
    public SqlKindMonitorInfo decode(ReadOnlyAstObject serial) {
        return new SqlKindMonitorInfo(
            orZero(decodeLong(serial, COUNT_KEY)),
            orZero(decodeLong(serial, TOTAL_NANOS_KEY)),
            orZero(decodeLong(serial, ERROR_COUNT_KEY)),
            decodeInteger(serial, WAITING_KEY, 0),
            decodeInteger(serial, EXECUTING_KEY, 0),
            decodeInteger(serial, MAX_CONCURRENCY_KEY, 0),
            decodeInteger(serial, PEAK_WAITING_KEY, 0),
            decodeInteger(serial, SHED_KEY, 0)); // absent on older servers -> 0
    }

    private static long orZero(Long l) {
        return l == null ? 0L : l;
    }
}
