package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.BootJobFailureMonitorInfo;
import dev.webfx.stack.db.querypush.CompressionMonitorInfo;
import dev.webfx.stack.db.querypush.NameCountInfo;
import dev.webfx.stack.db.querypush.QueryPushMonitorInfo;
import dev.webfx.stack.db.querypush.QueryStreamMonitorInfo;
import dev.webfx.stack.db.querypush.SqlExecutionMonitorInfo;
import dev.webfx.stack.db.querypush.SystemResourceMonitorInfo;

public final class QueryPushMonitorInfoSerialCodec extends SerialCodecBase<QueryPushMonitorInfo> {

    private static final String CODEC_ID = "QueryPushMonitorInfo";
    private static final String PUSH_CLIENTS_COUNT_KEY = "pushClientsCount";
    private static final String SUBSCRIBED_USERS_COUNT_KEY = "subscribedUsersCount";
    private static final String QUERY_STREAMS_KEY = "queryStreams";
    private static final String SQL_EXECUTION_KEY = "sqlExecution";
    private static final String COMPRESSION_KEY = "compression";
    private static final String CLIENT_VERSIONS_KEY = "clientVersions";
    private static final String CLIENT_PWA_MODES_KEY = "clientPwaModes";
    private static final String CLIENT_BROWSERS_KEY = "clientBrowsers";
    private static final String CLIENT_OSES_KEY = "clientOses";
    private static final String CLIENT_DEVICE_TYPES_KEY = "clientDeviceTypes";
    private static final String CLIENT_SIGN_IN_STATUSES_KEY = "clientSignInStatuses";
    private static final String CLIENT_APPS_KEY = "clientApps";
    private static final String SYSTEM_RESOURCE_KEY = "systemResource";
    private static final String BOOT_FAILURES_KEY = "bootFailures";

    public QueryPushMonitorInfoSerialCodec() {
        super(QueryPushMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(QueryPushMonitorInfo arg, AstObject serial) {
        encodeInteger(serial, PUSH_CLIENTS_COUNT_KEY,     arg.getPushClientsCount());
        encodeInteger(serial, SUBSCRIBED_USERS_COUNT_KEY, arg.getSubscribedUsersCount());
        encodeArray(  serial, QUERY_STREAMS_KEY,          arg.getQueryStreams());
        // Null on older servers — encodeObject skips nulls, decodeObject returns null.
        encodeObject( serial, SQL_EXECUTION_KEY,          arg.getSqlExecution());
        encodeObject( serial, COMPRESSION_KEY,            arg.getCompression());
        encodeObject( serial, SYSTEM_RESOURCE_KEY,        arg.getSystemResource());
        encodeArray(  serial, CLIENT_VERSIONS_KEY,        arg.getClientVersions());
        encodeArray(  serial, CLIENT_PWA_MODES_KEY,       arg.getClientPwaModes());
        encodeArray(  serial, CLIENT_BROWSERS_KEY,        arg.getClientBrowsers());
        encodeArray(  serial, CLIENT_OSES_KEY,            arg.getClientOses());
        encodeArray(  serial, CLIENT_DEVICE_TYPES_KEY,    arg.getClientDeviceTypes());
        encodeArray(  serial, CLIENT_SIGN_IN_STATUSES_KEY, arg.getClientSignInStatuses());
        encodeArray(  serial, CLIENT_APPS_KEY,            arg.getClientApps());
        // Null/empty on a clean boot — encodeArray skips null, decodeArray returns null on older servers.
        encodeArray(  serial, BOOT_FAILURES_KEY,          arg.getBootFailures());
    }

    @Override
    public QueryPushMonitorInfo decode(ReadOnlyAstObject serial) {
        return new QueryPushMonitorInfo(
                decodeInteger(serial, PUSH_CLIENTS_COUNT_KEY, 0),
                decodeInteger(serial, SUBSCRIBED_USERS_COUNT_KEY, 0),
                decodeArray(  serial, QUERY_STREAMS_KEY, QueryStreamMonitorInfo.class),
                (SqlExecutionMonitorInfo) decodeObject(serial, SQL_EXECUTION_KEY),
                (CompressionMonitorInfo) decodeObject(serial, COMPRESSION_KEY),
                decodeArray(  serial, CLIENT_VERSIONS_KEY, NameCountInfo.class),
                decodeArray(  serial, CLIENT_PWA_MODES_KEY, NameCountInfo.class),
                decodeArray(  serial, CLIENT_BROWSERS_KEY, NameCountInfo.class),
                decodeArray(  serial, CLIENT_OSES_KEY, NameCountInfo.class),
                decodeArray(  serial, CLIENT_DEVICE_TYPES_KEY, NameCountInfo.class),
                decodeArray(  serial, CLIENT_SIGN_IN_STATUSES_KEY, NameCountInfo.class),
                decodeArray(  serial, CLIENT_APPS_KEY, NameCountInfo.class),
                (SystemResourceMonitorInfo) decodeObject(serial, SYSTEM_RESOURCE_KEY),
                decodeArray(  serial, BOOT_FAILURES_KEY, BootJobFailureMonitorInfo.class)
        );
    }

}
