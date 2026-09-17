package dev.webfx.stack.db.datasource;

/**
 * @author Bruno Salmon
 */
public class ConnectionDetails {

    private final String url;
    private final String host;
    private final int port;
    private final String filePath;
    private final String databaseName;
    private final String username;
    private final String password;
    // Max parallel connections for the read (query) and write (submit) pools; -1 = unset, letting the
    // connected provider apply its own default. Carried here so pool sizing can come from the datasource
    // configuration without the provider modules depending on the configuration API.
    private final int queryPoolSize;
    private final int submitPoolSize;

    public ConnectionDetails(String filePath, String databaseName, String username, String password) {
        this(null, -1, filePath, databaseName, null, username, password);
    }

    public ConnectionDetails(String host, int port, String databaseName, String username, String password) {
        this(host, port, null, databaseName, null, username, password);
    }

    public ConnectionDetails(String host, int port, String filePath, String databaseName, String url, String username, String password) {
        this(host, port, filePath, databaseName, url, username, password, -1, -1);
    }

    public ConnectionDetails(String host, int port, String filePath, String databaseName, String url, String username, String password, int queryPoolSize, int submitPoolSize) {
        this.host = host;
        this.port = port;
        this.filePath = filePath;
        this.databaseName = databaseName;
        this.url = url;
        this.username = username;
        this.password = password;
        this.queryPoolSize = queryPoolSize;
        this.submitPoolSize = submitPoolSize;
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getQueryPoolSize() {
        return queryPoolSize;
    }

    public int getSubmitPoolSize() {
        return submitPoolSize;
    }

}
