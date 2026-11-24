package dev.webfx.stack.db.query;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tables and columns a CLIENT's query may never touch, whatever it selects, filters or joins on.
 *
 * <h3>Why names at the SQL level</h3>
 *
 * <p>Matched against the table and column names the query compiler actually emits, not against the words in
 * the query. A DQL statement can reach a column without naming its table — {@code person.frontendAccount.password}
 * reads a password from a query rooted at {@code Document} — and can touch one without selecting it, in a
 * {@code where} clause that lets a client test guesses one row at a time. What reaches SQL is the only thing
 * that cannot be rephrased, so that is what is checked. See the inspector that enforces this.
 *
 * <h3>What belongs here</h3>
 *
 * <p>Secrets whose disclosure is a way in, not merely a leak: sign-in tokens and codes, password hashes, reset
 * tokens. This is not the place for ordinary personal data, which needs a real read-authorisation model rather
 * than a list — a list long enough to cover it would be a policy nobody could review.
 *
 * <p>Plain collections and copy-on-write rather than concurrent ones, because this class is compiled into the
 * browser as well (the query endpoints that consult it are registered in the GWT entry point) and only the
 * JRE that GWT emulates is available. Entries are added at boot and read on every client query, which is the
 * access pattern copy-on-write suits anyway.
 *
 * @author Claude Code
 */
public final class ClientReadDenyList {

    private static volatile Set<String> deniedTables = Collections.emptySet();
    /** "table.column", lower-cased. */
    private static volatile Set<String> deniedColumns = Collections.emptySet();
    /** "table.column", lower-cased — testable by equality, never readable. See denyColumnExceptEqualityMatch. */
    private static volatile Set<String> capabilityColumns = Collections.emptySet();
    /** "table.column", lower-cased — WATCHED, never refused. See observeColumnExceptEqualityMatch. */
    private static volatile Set<String> observedCapabilityColumns = Collections.emptySet();
    /** "table.column", lower-cased — WATCHED flat, never refused and with no equality exception. See observeColumn. */
    private static volatile Set<String> observedColumns = Collections.emptySet();
    /** Bumped on every change, so anything that caches a verdict can tell when it went stale. */
    private static volatile int version;

    private ClientReadDenyList() {}

    /** Denies every column of this table to client queries — for tables that are nothing but secrets. */
    public static synchronized void denyTable(String sqlTableName) {
        Set<String> next = new HashSet<>(deniedTables);
        if (next.add(normalise(sqlTableName))) {
            deniedTables = Collections.unmodifiableSet(next);
            version++;
        }
    }

    /**
     * A column a client may TEST but never READ — the shape a capability token needs.
     *
     * <h3>Why a third category, and not simply a denial</h3>
     *
     * <p>A capability token is a secret the holder is supposed to present. An invitation link, a volunteer's
     * proposed dates, an arrival confirmation: the page is reached by whoever holds the link, and the server
     * finds the row by the token in it. So the column cannot be denied outright — the feature IS looking a row
     * up by it — and it cannot be left readable either, because <b>the way a capability like this is defeated is
     * not by guessing one but by asking for all of them.</b> {@code select token from Invitation} returns every
     * live token, and the tokens are random precisely so that nobody has to guess.
     *
     * <p>So: comparing it for equality against a bound parameter is allowed — as many times in one statement as
     * the caller likes, which is guessing at whatever rate the transport allows and is worth nothing against a
     * random token — and everything else is refused —
     * selecting it, ordering by it, testing it with {@code like} or a range, grouping on it. What that leaves a
     * caller is one bit per request, "does this exact token exist", which is what presenting a capability
     * legitimately tells you and is worth nothing against a random one.
     *
     * <p><b>The check is fail-closed on anything it does not recognise</b>, which is what makes a rule this
     * specific safe to state: a statement naming one of these columns in a construct the check cannot read is
     * refused rather than guessed at. The population of such statements is tiny and known, so a false refusal
     * shows up at once rather than lurking.
     */
    public static synchronized void denyColumnExceptEqualityMatch(String sqlTableName, String sqlColumnName) {
        // Declaring a column both ways would SILENTLY WEAKEN it: the reader asks isCapabilityColumn first, so
        // the testable rule would win and the absolute denial beside it would never fire. Refused at boot, where
        // it is one stack trace, rather than discovered later as a column everyone believed was unreadable.
        if (deniedColumns.contains(normalise(sqlTableName) + "." + normalise(sqlColumnName))
            || isTableDenied(sqlTableName))
            throw new IllegalStateException(
                "A column already denied to clients cannot also be declared testable: " + sqlTableName);
        Set<String> next = new HashSet<>(capabilityColumns);
        if (next.add(normalise(sqlTableName) + "." + normalise(sqlColumnName))) {
            capabilityColumns = Collections.unmodifiableSet(next);
            version++;
        }
        // Enforcing SUPERSEDES watching, so the two sets cannot both hold a column whichever order they are
        // declared in. Watching what is already refused reports a read that never happened, and the watch's
        // whole purpose is a count somebody will act on. Silent rather than a throw in this direction, because
        // a deny arriving after a watch is the INTENDED end state — it is the restoration this was built for.
        dropObserved(sqlTableName, sqlColumnName);
    }

    private static void dropObserved(String sqlTableName, String sqlColumnName) {
        String key = normalise(sqlTableName) + "." + normalise(sqlColumnName);
        if (observedCapabilityColumns.contains(key)) {
            Set<String> next = new HashSet<>(observedCapabilityColumns);
            next.remove(key);
            observedCapabilityColumns = Collections.unmodifiableSet(next);
            version++;
        }
    }

    /** Whether this column is a capability token: testable by equality, never readable. */
    public static boolean isCapabilityColumn(String sqlTableName, String sqlColumnName) {
        return sqlTableName != null && sqlColumnName != null
               && capabilityColumns.contains(normalise(sqlTableName) + "." + normalise(sqlColumnName));
    }

    /**
     * The same rule as {@link #denyColumnExceptEqualityMatch}, WATCHED rather than enforced: a client that
     * reads this column is reported and served.
     *
     * <h3>What this is for, which is a question the enforced rule cannot answer</h3>
     *
     * <p>Turning the enforced rule on breaks every client still sending the old statement, and we found that
     * out the expensive way — {@code invitation.token} refused real invitees for fifteen hours, each one told
     * their invitation was invalid, because an emailed link is a FIRST page load served from a cached shell.
     * The rule was paused to let the bundles age out.
     *
     * <p>Pausing it also removed the only instrument. The refusal WAS the detection, so with the rule off
     * there is no way to tell whether the stale clients have gone, and switching it back on to find out costs
     * another round of broken invitations. That is a guess dressed as a date.
     *
     * <p>So: declare the column here instead, learn the answer for free, and restore the enforced rule when
     * the count has been zero long enough to mean something. <b>This can refuse nothing</b> — it is evaluated
     * on the observation path, which the registry's own contract keeps separate from the verdict, and it is
     * consulted by a reader that denies nothing at all.
     */
    public static synchronized void observeColumnExceptEqualityMatch(String sqlTableName, String sqlColumnName) {
        // Both at once is a mistake worth catching at boot: the enforced rule already refuses and logs, so an
        // observation beside it reports a read that never happened. It usually means a pause was restored and
        // the watch left behind.
        if (isCapabilityColumn(sqlTableName, sqlColumnName) || isColumnDenied(sqlTableName, sqlColumnName))
            throw new IllegalStateException(
                "A column already denied to clients cannot also be merely observed: " + sqlTableName);
        if (isObservedColumn(sqlTableName, sqlColumnName))
            throw new IllegalStateException(
                "A column cannot be watched both flat and as a capability token: " + sqlTableName);
        Set<String> next = new HashSet<>(observedCapabilityColumns);
        if (next.add(normalise(sqlTableName) + "." + normalise(sqlColumnName))) {
            observedCapabilityColumns = Collections.unmodifiableSet(next);
            version++;
        }
    }

    /** Whether this column is being WATCHED as a capability token — reported when read, never refused. */
    public static boolean isObservedCapabilityColumn(String sqlTableName, String sqlColumnName) {
        return sqlTableName != null && sqlColumnName != null
               && observedCapabilityColumns.contains(normalise(sqlTableName) + "." + normalise(sqlColumnName));
    }

    /** True when nothing is being watched, so the observation pass has nothing to look for and is skipped. */
    public static boolean hasNoObservedColumns() {
        return observedCapabilityColumns.isEmpty() && observedColumns.isEmpty();
    }

    /**
     * Watches a column that will be denied FLAT — reported when a client reads it, and served.
     *
     * <p>The twin of {@link #observeColumnExceptEqualityMatch}, for the other kind of secret. That one is for a
     * capability token, where looking a row up BY the value is the legitimate use and only READING it is the
     * leak, so its watch reports everything the equality walk cannot account for. This one is for a secret with
     * no legitimate use from a client at all — an API key, where an equality test would itself be a
     * per-character oracle — so every appearance is a read and there is nothing to except.
     *
     * <h3>Why it exists, which is a gap found by falling into it</h3>
     *
     * <p>{@code invitation.token} was restored on evidence: paused, watched, and denied again once the count had
     * been zero for a day. {@code organization.bunny_api_key} was denied on 2026-10-30 with no evidence at all,
     * because a flat-denied column had no watch — the list could deny it or say nothing, and the only way to
     * find out whether a client still read it was to deny it and wait for a complaint. That is the guess dressed
     * as a date the capability watch was written to avoid, and it applied just as much here.
     *
     * <p><b>Counted separately from the capability watch, and that is not tidiness.</b> The enforcing walk
     * reconciles a hit count against the equality tests it can point at, so a flat hit mixed into that count
     * would be read as an unaccounted-for capability read — or worse, excused by an equality test somewhere
     * else in the same statement. Different question, different tally.
     */
    public static synchronized void observeColumn(String sqlTableName, String sqlColumnName) {
        // Same boot-time guard as the capability watch, for the same reason: the enforced rule already refuses
        // and logs, so an observation beside it reports a read that never happened. This is the error that
        // aborted a deploy on 2026-10-30 when a restored deny was left beside its watch.
        if (isColumnDenied(sqlTableName, sqlColumnName))
            throw new IllegalStateException(
                "A column already denied to clients cannot also be merely observed: " + sqlTableName);
        if (isObservedCapabilityColumn(sqlTableName, sqlColumnName))
            throw new IllegalStateException(
                "A column cannot be watched both flat and as a capability token: " + sqlTableName);
        Set<String> next = new HashSet<>(observedColumns);
        if (next.add(normalise(sqlTableName) + "." + normalise(sqlColumnName))) {
            observedColumns = Collections.unmodifiableSet(next);
            version++;
        }
    }

    /** Whether this column is being WATCHED flat — reported on any read, never refused, no equality exception. */
    public static boolean isObservedColumn(String sqlTableName, String sqlColumnName) {
        return sqlTableName != null && sqlColumnName != null
               && observedColumns.contains(normalise(sqlTableName) + "." + normalise(sqlColumnName));
    }

    /** Denies one column to client queries, leaving the rest of its table readable. */
    public static synchronized void denyColumn(String sqlTableName, String sqlColumnName) {
        Set<String> next = new HashSet<>(deniedColumns);
        if (next.add(normalise(sqlTableName) + "." + normalise(sqlColumnName))) {
            deniedColumns = Collections.unmodifiableSet(next);
            version++;
        }
    }

    public static boolean isTableDenied(String sqlTableName) {
        return sqlTableName != null && deniedTables.contains(normalise(sqlTableName));
    }

    /**
     * Whether a client may read this column at all.
     *
     * <p>True for a capability column too, deliberately: a consumer that has not learned about the third
     * category should refuse one rather than hand it over. The one place that knows better asks
     * {@link #isCapabilityColumn} FIRST, and it is the only place that may.
     */
    public static boolean isColumnDenied(String sqlTableName, String sqlColumnName) {
        if (sqlTableName == null || sqlColumnName == null)
            return false;
        return isTableDenied(sqlTableName)
               || deniedColumns.contains(normalise(sqlTableName) + "." + normalise(sqlColumnName))
               || isCapabilityColumn(sqlTableName, sqlColumnName);
    }

    /**
     * True when nothing has been declared secret, so there is nothing for an inspector to enforce.
     *
     * <p>Watched columns are deliberately NOT counted. They enforce nothing, and a deployment that only
     * watches must not thereby switch the enforcement path on — that would turn an observation into the
     * fail-closed refusal it exists to avoid.
     */
    public static boolean isEmpty() {
        return deniedTables.isEmpty() && deniedColumns.isEmpty() && capabilityColumns.isEmpty();
    }

    public static int version() {
        return version;
    }

    /**
     * Lower-cased and stripped of quoting and schema, because the compiler may emit {@code public."MagicLink"}
     * where the list says {@code magic_link}. A mismatch here fails OPEN — the secret stays readable — so the
     * comparison is made as forgiving as the names allow.
     */
    private static String normalise(String name) {
        String n = name.trim().toLowerCase();
        int dot = n.lastIndexOf('.');
        if (dot >= 0)
            n = n.substring(dot + 1);
        return n.replace("\"", "");
    }
}
