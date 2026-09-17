package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.BootJobFailureMonitorInfo;

public final class BootJobFailureMonitorInfoSerialCodec extends SerialCodecBase<BootJobFailureMonitorInfo> {

    private static final String CODEC_ID = "BootJobFailureMonitorInfo";
    private static final String EPOCH_MILLIS_KEY = "epochMillis";
    private static final String PHASE_KEY = "phase";
    private static final String JOB_NAME_KEY = "jobName";
    private static final String MESSAGE_KEY = "message";
    private static final String STACK_TRACE_KEY = "stackTrace";

    public BootJobFailureMonitorInfoSerialCodec() {
        super(BootJobFailureMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(BootJobFailureMonitorInfo arg, AstObject serial) {
        encodeLong(  serial, EPOCH_MILLIS_KEY, arg.getEpochMillis());
        encodeString(serial, PHASE_KEY,        arg.getPhase());
        encodeString(serial, JOB_NAME_KEY,     arg.getJobName());
        encodeString(serial, MESSAGE_KEY,      arg.getMessage());
        encodeString(serial, STACK_TRACE_KEY,  arg.getStackTrace());
    }

    @Override
    public BootJobFailureMonitorInfo decode(ReadOnlyAstObject serial) {
        return new BootJobFailureMonitorInfo(
            orZero(decodeLong(serial, EPOCH_MILLIS_KEY)),
            decodeString(serial, PHASE_KEY),
            decodeString(serial, JOB_NAME_KEY),
            decodeString(serial, MESSAGE_KEY),
            decodeString(serial, STACK_TRACE_KEY));
    }

    private static long orZero(Long l) {
        return l == null ? 0L : l;
    }
}
