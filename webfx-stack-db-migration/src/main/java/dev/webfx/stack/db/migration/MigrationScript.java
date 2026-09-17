package dev.webfx.stack.db.migration;

import java.util.List;

/**
 * A database migration script bundled in the application jar, identified by its version number (parsed from
 * its {@code V0007__description.sql} file name). The script text is already split into individual statements
 * (the SubmitService API executes one SQL command per statement) and checksummed, so the runner can detect
 * when an already-applied script has been modified after shipping.
 *
 * @author Bruno Salmon
 */
public final class MigrationScript {

    private final int version;
    private final String fileName;
    private final String checksum;
    private final String sql;
    private final List<String> statements;

    public MigrationScript(int version, String fileName, String checksum, String sql, List<String> statements) {
        this.version = version;
        this.fileName = fileName;
        this.checksum = checksum;
        this.sql = sql;
        this.statements = statements;
    }

    public int getVersion() {
        return version;
    }

    public String getFileName() {
        return fileName;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getSql() {
        return sql;
    }

    public List<String> getStatements() {
        return statements;
    }

    @Override
    public String toString() {
        return fileName;
    }
}
