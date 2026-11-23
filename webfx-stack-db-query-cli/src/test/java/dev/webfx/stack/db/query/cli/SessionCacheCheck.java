package dev.webfx.stack.db.query.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;

import static dev.webfx.stack.db.query.cli.Checks.check;
import static dev.webfx.stack.db.query.cli.Checks.report;
import static dev.webfx.stack.db.query.cli.Checks.section;

/**
 * That the one new secret this tool creates stays owner-only, short-lived, and out of every log line.
 *
 * <h3>Why each of these is a check and not a convention</h3>
 *
 * <ul>
 *   <li><b>Mode 600 on write</b> is easy to get right once and easy to lose: a later rewrite of the file
 *       through a different code path, or a create-then-chmod, leaves a window or a wrong mode behind.</li>
 *   <li><b>Mode 600 on READ</b> is the half that actually protects anything. A file created once with the
 *       wrong umask stays wrong for ever and nothing notices; refusing to present it is what makes the write
 *       side's correctness verifiable after the fact.</li>
 *   <li><b>Expiry</b> bounds how long a leaked file is worth stealing. Thirty minutes is the server's access
 *       window, so a cache older than that is already refused by the server — expiring it here turns an
 *       unexplained refusal into a sign-in prompt.</li>
 *   <li><b>Redaction</b> because the entry is the kind of object that ends up in a log line by accident, and
 *       the field it carries is a bearer credential.</li>
 * </ul>
 *
 * <p><b>Probed by deletion:</b> removing the permission check from {@code load}, the expiry comparison, and
 * the {@code toString} override each made this check fail; all three were restored.
 *
 * @author Claude Code
 */
public class SessionCacheCheck {

    static final long NOW = 1_700_000_000_000L;

    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("read-scope-cli-check");
        directory.toFile().deleteOnExit();
        SessionCache cache = new SessionCache(directory);
        SessionCache.Entry entry = new SessionCache.Entry("a-token-shaped-string", "operator",
                                                          Audience.BACK_OFFICE, "wss://example:443", NOW);

        section("The file name is the tool's, never a caller's");
        check("the cache sits at conf/" + SessionCache.FILE_NAME,
              cache.file().equals(directory.resolve(SessionCache.FILE_NAME)));
        check("and the name is a constant the deny rules can match on",
              "read-scope-cli-session.properties".equals(SessionCache.FILE_NAME));

        section("Nothing cached is nothing presented");
        check("an absent cache reads as no session", cache.load(NOW) == null);

        section("A stored session comes back, and only to its owner");
        cache.store(entry);
        check("the file exists", Files.exists(cache.file()));
        check("it is mode 600", "rw-------".equals(modeOf(cache.file())));
        check("it has no permission problem", cache.permissionProblem() == null);
        SessionCache.Entry loaded = cache.load(NOW);
        check("the token comes back", loaded != null && entry.token().equals(loaded.token()));
        check("so do the caller, the audience and the server, which a run needs to refuse the wrong cache",
              loaded != null && "operator".equals(loaded.username())
              && loaded.audience() == Audience.BACK_OFFICE
              && "wss://example:443".equals(loaded.server()));

        section("A cache anyone else can read is refused, not used");
        setMode(cache.file(), "rw-r--r--");
        check("a group/world-readable cache is reported as a problem", cache.permissionProblem() != null);
        boolean refused = false;
        try {
            cache.load(NOW);
        } catch (IOException e) {
            refused = e.getMessage().contains("chmod 600");
        }
        check("and loading it throws, naming the fix", refused);
        check("the file is still there — deleting it would hide the misconfiguration", Files.exists(cache.file()));

        section("A session past the server's access window is deleted, not presented");
        cache.store(entry); // re-store, which also restores the mode
        check("re-storing restores mode 600", "rw-------".equals(modeOf(cache.file())));
        check("one millisecond before the TTL it is still presented",
              cache.load(NOW + SessionCache.TTL_MILLIS - 1) != null);
        cache.store(entry);
        check("at exactly the TTL it is gone",
              cache.load(NOW + SessionCache.TTL_MILLIS) == null);
        check("and the stale file has been deleted rather than left on disk", !Files.exists(cache.file()));
        check("the TTL is the server's 30-minute access window, not a number of its own",
              SessionCache.TTL_MILLIS == 30 * 60 * 1000L);

        section("A malformed cache is cleared rather than half-trusted");
        Files.writeString(cache.file(), "token=\nstoredAt=notanumber\n");
        setMode(cache.file(), "rw-------");
        check("a cache with no usable token reads as no session", cache.load(NOW) == null);
        check("and is deleted", !Files.exists(cache.file()));

        section("A symbolic link at the cache path is refused, not written through");
        cache.clear();
        Path elsewhere = directory.resolve("elsewhere.properties");
        Files.writeString(elsewhere, "untouched\n");
        Files.createSymbolicLink(cache.file(), elsewhere);
        check("storing through a link is refused", refusesIO(() -> cache.store(entry)));
        check("and loading through one is refused", refusesIO(() -> cache.load(NOW)));
        check("the file it pointed at is untouched",
              "untouched\n".equals(Files.readString(elsewhere)));
        Files.delete(cache.file());
        Files.delete(elsewhere);

        section("Sign-out leaves nothing behind");
        cache.store(entry);
        cache.clear();
        check("the cache is gone after clear()", !Files.exists(cache.file()));
        check("and clearing again is not an error", clearTwiceSucceeds(cache));

        section("The token never reaches a log line through toString");
        String printed = entry.toString();
        check("toString does not contain the token", !printed.contains("a-token-shaped-string"));
        check("toString says the token is redacted", printed.contains("***"));
        check("but it still names the caller, so a log line identifies the session",
              printed.contains("operator"));

        System.exit(report("SessionCacheCheck"));
    }

    interface IoAction {
        void run() throws IOException;
    }

    static boolean refusesIO(IoAction action) {
        try {
            action.run();
            return false;
        } catch (IOException e) {
            return e.getMessage() != null && e.getMessage().contains("symbolic link");
        }
    }

    static boolean clearTwiceSucceeds(SessionCache cache) {
        try {
            cache.clear();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static String modeOf(Path file) throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
        return view == null ? "rw-------" /* nothing to check on this filesystem */
                            : PosixFilePermissions.toString(view.readAttributes().permissions());
    }

    static void setMode(Path file, String mode) throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
        if (view != null)
            view.setPermissions(PosixFilePermissions.fromString(mode));
    }
}
