package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.CompressionMonitorInfo;

public final class CompressionMonitorInfoSerialCodec extends SerialCodecBase<CompressionMonitorInfo> {

    private static final String CODEC_ID = "CompressionMonitorInfo";
    private static final String COUNT_KEY = "count";
    private static final String TOTAL_NANOS_KEY = "totalNanos";
    private static final String MAX_NANOS_KEY = "maxNanos";
    private static final String SLOW_COUNT_KEY = "slowCount";
    private static final String TOTAL_CELLS_KEY = "totalCells";

    public CompressionMonitorInfoSerialCodec() {
        super(CompressionMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(CompressionMonitorInfo arg, AstObject serial) {
        encodeLong(serial, COUNT_KEY,       arg.getCount());
        encodeLong(serial, TOTAL_NANOS_KEY, arg.getTotalNanos());
        encodeLong(serial, MAX_NANOS_KEY,   arg.getMaxNanos());
        encodeLong(serial, SLOW_COUNT_KEY,  arg.getSlowCount());
        encodeLong(serial, TOTAL_CELLS_KEY, arg.getTotalCells());
    }

    @Override
    public CompressionMonitorInfo decode(ReadOnlyAstObject serial) {
        return new CompressionMonitorInfo(
            orZero(decodeLong(serial, COUNT_KEY)),
            orZero(decodeLong(serial, TOTAL_NANOS_KEY)),
            orZero(decodeLong(serial, MAX_NANOS_KEY)),
            orZero(decodeLong(serial, SLOW_COUNT_KEY)),
            orZero(decodeLong(serial, TOTAL_CELLS_KEY)));
    }

    private static long orZero(Long l) {
        return l == null ? 0L : l;
    }
}
