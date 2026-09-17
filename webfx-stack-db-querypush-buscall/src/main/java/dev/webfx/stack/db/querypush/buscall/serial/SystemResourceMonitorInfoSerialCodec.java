package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.SystemResourceMonitorInfo;

public final class SystemResourceMonitorInfoSerialCodec extends SerialCodecBase<SystemResourceMonitorInfo> {

    private static final String CODEC_ID = "SystemResourceMonitorInfo";
    // Wire keys MUST match the client (@kbs3/shared) field names — the React client reads them by name
    // off the decoded payload, with no remapping. CPU load is carried as a per-mille integer (0..1000,
    // or -1 = unavailable): SerialCodecBase has no double primitive, and per-mille is ample for a CPU %.
    private static final String CPU_PERMILLE_KEY = "processCpuPermille";
    private static final String PROCESSORS_KEY = "availableProcessors";
    private static final String HEAP_USED_KEY = "heapUsed";
    private static final String HEAP_COMMITTED_KEY = "heapCommitted";
    private static final String HEAP_MAX_KEY = "heapMax";
    private static final String OLD_GEN_AFTER_GC_KEY = "oldGenUsedAfterGc";
    private static final String GC_COUNT_KEY = "gcCount";
    private static final String GC_TIME_MILLIS_KEY = "gcTimeMillis";
    private static final String UPTIME_MILLIS_KEY = "uptimeMillis";
    private static final String EVENT_LOOP_LAG_MILLIS_KEY = "eventLoopLagMillis";
    private static final String SERVER_VERSION_KEY = "serverVersion";

    public SystemResourceMonitorInfoSerialCodec() {
        super(SystemResourceMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(SystemResourceMonitorInfo arg, AstObject serial) {
        double load = arg.getProcessCpuLoad();
        encodeInteger(serial, CPU_PERMILLE_KEY, load < 0 ? -1 : (int) Math.round(load * 1000));
        encodeInteger(serial, PROCESSORS_KEY,        arg.getAvailableProcessors());
        encodeLong(   serial, HEAP_USED_KEY,         arg.getHeapUsed());
        encodeLong(   serial, HEAP_COMMITTED_KEY,    arg.getHeapCommitted());
        encodeLong(   serial, HEAP_MAX_KEY,          arg.getHeapMax());
        encodeLong(   serial, OLD_GEN_AFTER_GC_KEY,  arg.getOldGenUsedAfterGc());
        encodeLong(   serial, GC_COUNT_KEY,          arg.getGcCount());
        encodeLong(   serial, GC_TIME_MILLIS_KEY,    arg.getGcTimeMillis());
        encodeLong(   serial, UPTIME_MILLIS_KEY,     arg.getUptimeMillis());
        encodeLong(   serial, EVENT_LOOP_LAG_MILLIS_KEY, arg.getEventLoopLagMillis());
        encodeString( serial, SERVER_VERSION_KEY,    arg.getServerVersion());
    }

    @Override
    public SystemResourceMonitorInfo decode(ReadOnlyAstObject serial) {
        int cpuPermille = orInt(decodeInteger(serial, CPU_PERMILLE_KEY), -1);
        double load = cpuPermille < 0 ? -1 : cpuPermille / 1000d;
        return new SystemResourceMonitorInfo(
            load,
            orInt(decodeInteger(serial, PROCESSORS_KEY), 0),
            orZero(decodeLong(serial, HEAP_USED_KEY)),
            orZero(decodeLong(serial, HEAP_COMMITTED_KEY)),
            orLong(decodeLong(serial, HEAP_MAX_KEY), -1),
            orLong(decodeLong(serial, OLD_GEN_AFTER_GC_KEY), -1),
            orZero(decodeLong(serial, GC_COUNT_KEY)),
            orZero(decodeLong(serial, GC_TIME_MILLIS_KEY)),
            orZero(decodeLong(serial, UPTIME_MILLIS_KEY)),
            orLong(decodeLong(serial, EVENT_LOOP_LAG_MILLIS_KEY), -1),
            decodeString(serial, SERVER_VERSION_KEY));
    }

    private static int orInt(Integer i, int dflt) {
        return i == null ? dflt : i;
    }

    private static long orZero(Long l) {
        return l == null ? 0L : l;
    }

    private static long orLong(Long l, long dflt) {
        return l == null ? dflt : l;
    }
}
