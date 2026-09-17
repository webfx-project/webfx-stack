package dev.webfx.stack.db.querypush;

/**
 * Read-only monitoring snapshot of the server for the /monitor page, as returned by
 * {@link QueryPushService#getMonitorInfo()}: the query-push state (connected clients + the list
 * of push queries with their subscription details), plus database execution metrics
 * ({@link SqlExecutionMonitorInfo}) and QueryResult compression metrics
 * ({@link CompressionMonitorInfo}). Meant for display in an administration console.
 *
 * @author Bruno Salmon
 */
public final class QueryPushMonitorInfo {

    private final int pushClientsCount; // clients (~ browser tabs) currently registered on the push server
    private final int subscribedUsersCount; // distinct logged-in users holding at least one query-push subscription
    private final QueryStreamMonitorInfo[] queryStreams;
    private final SqlExecutionMonitorInfo sqlExecution; // read/write SQL execution metrics (may be null on older servers)
    private final CompressionMonitorInfo compression;   // QueryResult compression metrics (may be null on older servers)
    private final SystemResourceMonitorInfo systemResource; // JVM CPU + heap-memory metrics (may be null on older servers)
    private final NameCountInfo[] clientVersions;       // connected-clients breakdown by build version (may be null on older servers)
    private final NameCountInfo[] clientPwaModes;       // connected-clients breakdown by PWA mode: installed/browser/unknown (may be null on older servers)
    private final NameCountInfo[] clientBrowsers;       // connected-clients breakdown by browser (may be null on older servers)
    private final NameCountInfo[] clientOses;           // connected-clients breakdown by OS (may be null on older servers)
    private final NameCountInfo[] clientDeviceTypes;    // connected-clients breakdown by device type: desktop/tablet/phone/unknown (may be null on older servers)
    private final NameCountInfo[] clientSignInStatuses; // connected-clients (per connection) breakdown by sign-in status: anonymous / principal type name (may be null on older servers)
    private final NameCountInfo[] clientApps;           // connected-clients breakdown by app: bo (back-office) / fo (front-office) / unknown (may be null on older servers)
    private final BootJobFailureMonitorInfo[] bootFailures; // application jobs that failed during THIS task's boot — empty when it booted cleanly (may be null on older servers)

    public QueryPushMonitorInfo(int pushClientsCount, int subscribedUsersCount, QueryStreamMonitorInfo[] queryStreams,
                                SqlExecutionMonitorInfo sqlExecution, CompressionMonitorInfo compression,
                                NameCountInfo[] clientVersions, NameCountInfo[] clientPwaModes,
                                NameCountInfo[] clientBrowsers, NameCountInfo[] clientOses, NameCountInfo[] clientDeviceTypes,
                                NameCountInfo[] clientSignInStatuses, NameCountInfo[] clientApps, SystemResourceMonitorInfo systemResource,
                                BootJobFailureMonitorInfo[] bootFailures) {
        this.pushClientsCount = pushClientsCount;
        this.subscribedUsersCount = subscribedUsersCount;
        this.queryStreams = queryStreams;
        this.sqlExecution = sqlExecution;
        this.compression = compression;
        this.clientVersions = clientVersions;
        this.clientPwaModes = clientPwaModes;
        this.clientBrowsers = clientBrowsers;
        this.clientOses = clientOses;
        this.clientDeviceTypes = clientDeviceTypes;
        this.clientSignInStatuses = clientSignInStatuses;
        this.clientApps = clientApps;
        this.systemResource = systemResource;
        this.bootFailures = bootFailures;
    }

    public int getPushClientsCount() {
        return pushClientsCount;
    }

    public int getSubscribedUsersCount() {
        return subscribedUsersCount;
    }

    public QueryStreamMonitorInfo[] getQueryStreams() {
        return queryStreams;
    }

    public SqlExecutionMonitorInfo getSqlExecution() {
        return sqlExecution;
    }

    public CompressionMonitorInfo getCompression() {
        return compression;
    }

    public SystemResourceMonitorInfo getSystemResource() {
        return systemResource;
    }

    public NameCountInfo[] getClientVersions() {
        return clientVersions;
    }

    public NameCountInfo[] getClientPwaModes() {
        return clientPwaModes;
    }

    public NameCountInfo[] getClientBrowsers() {
        return clientBrowsers;
    }

    public NameCountInfo[] getClientOses() {
        return clientOses;
    }

    public NameCountInfo[] getClientDeviceTypes() {
        return clientDeviceTypes;
    }

    public NameCountInfo[] getClientSignInStatuses() {
        return clientSignInStatuses;
    }

    public NameCountInfo[] getClientApps() {
        return clientApps;
    }

    public BootJobFailureMonitorInfo[] getBootFailures() {
        return bootFailures;
    }
}
