package dev.webfx.stack.db.query.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;

/**
 * Where a signed-in session is kept between one-shot invocations — and the one new secret this tool creates.
 *
 * <h3>What is in here, stated plainly</h3>
 *
 * <p>A live session token at rest: a bearer credential for a real caller, which until it expires is that
 * caller. Anyone who can read this file can read whatever that caller can read, from anywhere. It exists
 * because one-shot invocation without a cache means a password prompt per probe, which in practice means
 * nobody runs the probes.
 *
 * <p>The arrangement copied here is {@code scripts/agent-db/psql-staging}'s: an agent may USE the cached
 * session, by running a probe, and may not READ the cache, because the path is denied to it. That split is
 * what makes a cached credential acceptable at all, and it only holds while the CLI never prints the token
 * and never takes its path from an argument — both of which are properties of this class, not conventions.
 *
 * <h3>The four things that bound it</h3>
 *
 * <ul>
 *   <li><b>{@code conf/}, which is gitignored</b> — the directory this repository already keeps credentials
 *       in, so it is covered by the rules and habits that already exist rather than inventing a new place.</li>
 *   <li><b>Mode 600, enforced on write and CHECKED on read.</b> A cache that anyone else on the machine can
 *       read is refused rather than used: the check is the half that matters, because a file created once
 *       with the wrong umask stays wrong forever and nothing would otherwise notice.</li>
 *   <li><b>{@link #TTL_MILLIS}, thirty minutes.</b> Not a round number chosen for comfort — it is
 *       {@code SessionLifetime.ACCESS_WINDOW_BASE_MILLIS}, the window after which the server stops accepting
 *       the token for action anyway. A one-shot CLI holds no renewal loop, so a cache older than that is
 *       already worthless; expiring it here means a stale file is deleted rather than presented, and the
 *       operator gets a sign-in prompt instead of an unexplained refusal.</li>
 *   <li><b>A denied path.</b> The repository's {@code permissions.deny} rules and credential guard name this
 *       file, exactly as they name the datasource and secret-variables files.</li>
 * </ul>
 *
 * @author Claude Code
 */
public final class SessionCache {

    /** The file name, which is also the literal the deny rules and the credential guard match on. */
    public static final String FILE_NAME = "read-scope-cli-session.properties";

    /**
     * How long a cached session may be presented. See the class comment: this is the server's access window,
     * so it is the point past which the token would be refused in any case.
     */
    public static final long TTL_MILLIS = 30 * 60 * 1000L;

    private static final Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");

    private static final String TOKEN_KEY = "token";
    private static final String USERNAME_KEY = "username";
    private static final String AUDIENCE_KEY = "audience";
    private static final String SERVER_KEY = "server";
    private static final String STORED_AT_KEY = "storedAt";

    /**
     * A cached session. {@code token} is the secret; everything else is there so a run can refuse a cache that
     * belongs to a different caller, audience or server rather than silently probing as somebody else.
     */
    public record Entry(String token, String username, Audience audience, String server, long storedAtMillis) {
        /** Redacted, because this record is the kind of thing that ends up in a log line by accident. */
        @Override
        public String toString() {
            return "SessionCache.Entry[username=" + username + ", audience=" + (audience == null ? null : audience.optionValue())
                   + ", server=" + server + ", storedAt=" + storedAtMillis + ", token=***]";
        }
    }

    private final Path file;

    /**
     * @param confDirectory the {@code conf/} directory; the file name is this class's, never a caller's, so
     *                      that no command line can ever name the cache and so the deny rules have one literal
     *                      to match
     */
    public SessionCache(Path confDirectory) {
        this.file = confDirectory.resolve(FILE_NAME);
    }

    /** The cache file, for refusal messages. Naming it is fine; printing its contents is not. */
    public Path file() {
        return file;
    }

    /**
     * The cached session, or null when there is none to present.
     *
     * <p>Null covers "no file", "expired" and "unreadable", and in the first two cases it also DELETES, so a
     * dead token does not sit on disk until somebody remembers it. An over-permissive file is the one case
     * that throws instead: deleting it would hide a misconfiguration that is worth seeing, and using it would
     * be the thing this guards against.
     */
    public Entry load(long nowMillis) throws IOException {
        refuseSymbolicLink();
        if (!Files.exists(file))
            return null;
        String permissionProblem = permissionProblem();
        if (permissionProblem != null)
            throw new IOException("Refusing to read " + file + ": " + permissionProblem
                                  + ". It holds a live session token; fix it with: chmod 600 " + file);
        Properties properties = new Properties();
        try (var in = Files.newInputStream(file)) {
            properties.load(in);
        }
        String token = properties.getProperty(TOKEN_KEY);
        long storedAt = parseLong(properties.getProperty(STORED_AT_KEY));
        if (token == null || token.isBlank() || storedAt <= 0) {
            clear();
            return null;
        }
        if (nowMillis - storedAt >= TTL_MILLIS) {
            clear();
            return null;
        }
        return new Entry(token,
                         properties.getProperty(USERNAME_KEY),
                         Audience.ofOptionValue(properties.getProperty(AUDIENCE_KEY)),
                         properties.getProperty(SERVER_KEY),
                         storedAt);
    }

    /** Writes the session, creating the file mode 600 and re-applying that mode if it already existed. */
    public void store(Entry entry) throws IOException {
        refuseSymbolicLink();
        Files.createDirectories(file.getParent());
        Properties properties = new Properties();
        properties.setProperty(TOKEN_KEY, entry.token());
        if (entry.username() != null)
            properties.setProperty(USERNAME_KEY, entry.username());
        if (entry.audience() != null)
            properties.setProperty(AUDIENCE_KEY, entry.audience().optionValue());
        if (entry.server() != null)
            properties.setProperty(SERVER_KEY, entry.server());
        properties.setProperty(STORED_AT_KEY, String.valueOf(entry.storedAtMillis()));

        // Created with the mode already set rather than set afterwards: between a default-mode create and a
        // chmod there is a window in which the token is world-readable, and a window is all a credential needs.
        if (!Files.exists(file))
            Files.createFile(file, PosixFilePermissions.asFileAttribute(OWNER_ONLY));
        restrictPermissions();
        StringBuilder text = new StringBuilder();
        text.append("# Written by the read-scope CLI. A LIVE SESSION TOKEN: treat as a credential.\n");
        text.append("# Expires ").append(TTL_MILLIS / 60000).append(" minutes after storedAt; delete it with the tool's 'signout'.\n");
        for (String key : new String[] {TOKEN_KEY, USERNAME_KEY, AUDIENCE_KEY, SERVER_KEY, STORED_AT_KEY}) {
            String value = properties.getProperty(key);
            if (value != null)
                text.append(key).append('=').append(value).append('\n');
        }
        Files.write(file, text.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        restrictPermissions();
    }

    /** Deletes the cache, if there is one. Used by sign-out, by expiry, and after a rejected token. */
    public void clear() throws IOException {
        Files.deleteIfExists(file);
    }

    /**
     * Why this file's permissions make it unfit to hold a token, or null when they do not.
     *
     * <p>Returns null on a filesystem with no POSIX view at all rather than refusing: there is nothing to
     * check and nothing to fix, and failing there would make the tool unusable on that platform for a reason
     * unrelated to its own correctness.
     */
    public String permissionProblem() throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
        if (view == null)
            return null;
        Set<PosixFilePermission> permissions = view.readAttributes().permissions();
        StringBuilder problems = new StringBuilder();
        if (permissions.contains(PosixFilePermission.GROUP_READ) || permissions.contains(PosixFilePermission.GROUP_WRITE))
            problems.append("it is readable or writable by the group");
        if (permissions.contains(PosixFilePermission.OTHERS_READ) || permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
            if (problems.length() > 0)
                problems.append(" and ");
            problems.append("it is readable or writable by everybody");
        }
        return problems.length() == 0 ? null : problems.toString();
    }

    /**
     * Refuses to read or write through a symbolic link at the cache path.
     *
     * <p>{@code Files.exists} follows links, so a link planted here would be left in place, the mode would be
     * applied to whatever it points at, and the token would be written through it. Reaching that state needs
     * write access to this directory — which is the operator's own — so this is a floor rather than a
     * boundary. It is here because the file holds a bearer credential and the check costs one line: a
     * credential file should be the file it claims to be.
     */
    private void refuseSymbolicLink() throws IOException {
        if (Files.isSymbolicLink(file))
            throw new IOException("Refusing to use " + file + ": it is a symbolic link, and this file holds a"
                                  + " live session token. Remove the link; the tool creates the file itself.");
    }

    private void restrictPermissions() throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
        if (view != null)
            view.setPermissions(OWNER_ONLY);
    }

    private static long parseLong(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
