package dev.webfx.stack.db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads the migration scripts bundled in the application jar from an explicit index file (an index avoids
 * classpath folder scanning, which is unreliable inside a fat jar). The index lists one script file name per
 * line ({@code V0001__description.sql}, {@code #} comments and blank lines allowed), in strictly ascending
 * version order. Each script is checksummed (SHA-256 of its LF-normalized UTF-8 text), split into individual
 * statements, and linted: transaction control is rejected (the migration runner owns the single transaction
 * all scripts run in), and so are statements that can't run inside a transaction (those must be applied
 * manually instead).
 *
 * @author Bruno Salmon
 */
public final class MigrationScriptReader {

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("V(\\d{4})__([A-Za-z0-9_]+)\\.sql");

    // Statements rejected because the runner owns the transaction all scripts run in
    private static final String[] TRANSACTION_CONTROL_KEYWORDS = {
        "BEGIN", "COMMIT", "ROLLBACK", "END", "ABORT", "START TRANSACTION", "SAVEPOINT", "RELEASE",
        "SET TRANSACTION", "PREPARE TRANSACTION", "COMMIT PREPARED", "ROLLBACK PREPARED"
    };

    // Statements rejected because they can't run inside a transaction block
    private static final String[] NON_TRANSACTIONAL_KEYWORDS = {
        "VACUUM", "ALTER SYSTEM", "CREATE DATABASE", "DROP DATABASE", "CREATE TABLESPACE", "DROP TABLESPACE",
        "CREATE INDEX CONCURRENTLY", "CREATE UNIQUE INDEX CONCURRENTLY", "DROP INDEX CONCURRENTLY"
    };

    private MigrationScriptReader() {
    }

    /**
     * Reads the migration scripts listed in the given index resource. The loading class must live in the same
     * module as the resources (so they resolve in both fat-jar and IDE runs), and the index path is relative
     * to that class's package (e.g. {@code "scripts/index.txt"}); the script files must sit next to the index.
     */
    public static List<MigrationScript> readFromIndex(Class<?> loadingClass, String indexResourcePath) {
        String indexContent = readResource(loadingClass, indexResourcePath);
        int lastSlash = indexResourcePath.lastIndexOf('/');
        String directory = lastSlash < 0 ? "" : indexResourcePath.substring(0, lastSlash + 1);
        List<MigrationScript> scripts = new ArrayList<>();
        int lastVersion = 0;
        for (String line : indexContent.split("\n")) {
            String fileName = line.trim();
            if (fileName.isEmpty() || fileName.startsWith("#"))
                continue;
            Matcher matcher = FILE_NAME_PATTERN.matcher(fileName);
            if (!matcher.matches())
                throw new IllegalArgumentException("Invalid migration file name '" + fileName + "' in " + indexResourcePath + " (expected pattern: V0001__short_description.sql)");
            int version = Integer.parseInt(matcher.group(1));
            if (version <= lastVersion)
                throw new IllegalArgumentException("Migration versions must be strictly ascending in " + indexResourcePath + " but found '" + fileName + "' after version " + lastVersion);
            lastVersion = version;
            String sql = readResource(loadingClass, directory + fileName).replaceAll("\r\n?", "\n");
            List<SqlScriptParser.SqlStatement> sqlStatements;
            try {
                sqlStatements = SqlScriptParser.splitStatements(sql);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(fileName + ": " + e.getMessage(), e);
            }
            if (sqlStatements.isEmpty())
                throw new IllegalArgumentException(fileName + " contains no SQL statements");
            List<String> statements = new ArrayList<>(sqlStatements.size());
            for (SqlScriptParser.SqlStatement statement : sqlStatements) {
                lint(fileName, statement.getNormalized());
                statements.add(statement.getText());
            }
            scripts.add(new MigrationScript(version, fileName, sha256Hex(sql), sql, statements));
        }
        return scripts;
    }

    private static void lint(String fileName, String normalizedStatement) {
        for (String keyword : TRANSACTION_CONTROL_KEYWORDS) {
            if (startsWithKeyword(normalizedStatement, keyword))
                throw new IllegalArgumentException(fileName + " contains transaction control ('" + keyword + "'): the migration runner already runs all scripts in a single transaction");
        }
        for (String keyword : NON_TRANSACTIONAL_KEYWORDS) {
            if (startsWithKeyword(normalizedStatement, keyword))
                throw new IllegalArgumentException(fileName + " contains '" + keyword + "', which can't run inside the migration transaction: apply it manually instead");
        }
        if (startsWithKeyword(normalizedStatement, "REINDEX") && normalizedStatement.contains(" CONCURRENTLY"))
            throw new IllegalArgumentException(fileName + " contains 'REINDEX ... CONCURRENTLY', which can't run inside the migration transaction: apply it manually instead");
    }

    private static boolean startsWithKeyword(String normalizedStatement, String keyword) {
        return normalizedStatement.equals(keyword) || normalizedStatement.startsWith(keyword + " ");
    }

    private static String readResource(Class<?> loadingClass, String resourcePath) {
        try (InputStream is = loadingClass.getResourceAsStream(resourcePath)) {
            if (is == null)
                throw new IllegalArgumentException("Migration resource not found: " + resourcePath + " (relative to " + loadingClass.getName() + ")");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256Hex(String content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 is always available on the JRE
        }
    }
}
