package dev.webfx.stack.orm.domainmodel;

import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlCompiled;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compiled statements, kept per data scope and bounded in both directions.
 *
 * <p>Two dimensions, and they fail differently.
 *
 * <p><b>Statements.</b> The key is text a CLIENT chooses, so a caller minting variants grows this without
 * limit — the hazard the read inventory caps itself against in as many words. It was unbounded before scope
 * existed; scope only multiplies it. Capped by dropping the least recently used, which beats clearing in
 * exactly the case the cap exists for: a caller minting statements evicts entries either way, but the
 * application's own statements are used constantly and so survive, where clearing discards them alongside
 * the noise and recompiles everything at once.
 *
 * <p><b>Scope tokens.</b> Bounded only if a token names one of a fixed set of scopes. Hand it something
 * caller-derived — an organization-id set, say — and the cache grows combinatorially with the callers. That
 * is a mistake in our own code, never in traffic, so it REFUSES rather than evicting: a design error that
 * throws on the first few requests is cheaper than one found later in a heap dump. It cannot fire while no
 * scope provider is registered, because an unscoped compilation has no token at all.
 *
 * <p><b>Not synchronised, and that is safe by DEPLOYMENT rather than by design.</b> {@code VertxRunner}
 * deploys one verticle with default options, so every handler it registers shares one event-loop context:
 * compilation happens on a single thread, which the production logs bear out — every client-read line is
 * {@code vert.x-eventloop-thread-1}. Nothing in the query path leaves that thread; {@code executeBlocking}
 * appears only in the Stripe gateway, and these modules start no threads or executors of their own.
 *
 * <p>The change that would end that is {@code setInstances(n)} — the obvious way to use more cores, and a
 * one-liner whose author has no reason to think about a cache. If it is ever made, this map and the two
 * fields below it need revisiting first. Note that the codebase is not of one mind here: ClientReadInventory
 * uses ConcurrentHashMap and AtomicLong throughout, so the convention cannot be read off the code.
 *
 * <p>Not weakly or softly referenced. Weak keys would be collected almost at once — the key is a string
 * built per request and held by nothing else — so the cache would simply stop caching. Soft references clear
 * in bulk under memory pressure, which means every query recompiles at the moment the server is least able
 * to afford it.
 *
 * @author Claude Code
 */
final class ScopedCompilationCache {

    /**
     * Distinct scopes this will cache for before it concludes the token is not a named scope. Generous: a
     * system with more than this many genuine scopes has a different problem, and the number exists to catch
     * a token that varies per caller, which passes it within minutes rather than approaching it slowly.
     */
    static final int MAX_SCOPE_TOKENS = 64;

    /** Compiled statements held before the least recently used are dropped. */
    static final int MAX_COMPILED_STATEMENTS = 2000;

    /** Dropped in one pass when full, so the scan is paid once per that many inserts rather than per insert. */
    private static final int PURGE_FRACTION = 4; // a quarter

    /**
     * One compiled statement and when it was last wanted.
     *
     * <p>The mark is a field on the ENTRY, deliberately, and not the map's iteration order. An access-ordered
     * map restructures itself on every {@code get}, which would turn each cache HIT into a structural write
     * to a map that is not synchronised on a server that compiles from more than one thread — trading a
     * stale read for a corrupted map. Writing a long races harmlessly: the loser costs an entry its place in
     * the queue, and nothing else.
     */
    private static final class Entry {
        final SqlCompiled compiled;
        long lastUsed;

        Entry(SqlCompiled compiled, long tick) {
            this.compiled = compiled;
            this.lastUsed = tick;
        }
    }

    private final Map<String, Entry> compiled = new HashMap<>();
    private final Set<String> scopeTokens = new HashSet<>();
    /**
     * A logical clock, not a wall clock. Monotonic whatever the system time does, and two entries used in
     * the same millisecond still order — which a timestamp would not give, and ties are exactly what an
     * eviction order must not have.
     */
    private long tick;

    /** The cache key for a statement under a scope. Null token — the unscoped case — keys on the text alone. */
    static String keyOf(String statement, Object scopeToken) {
        return scopeToken == null ? statement : scopeToken + "\u0000" + statement;
    }

    SqlCompiled get(String key) {
        Entry entry = compiled.get(key);
        if (entry == null)
            return null;
        entry.lastUsed = ++tick; // a hit is still wanted; only this field moves
        return entry.compiled;
    }

    void put(String key, SqlCompiled sqlCompiled) {
        if (compiled.size() >= MAX_COMPILED_STATEMENTS)
            purgeLeastRecentlyUsed();
        compiled.put(key, new Entry(sqlCompiled, ++tick));
    }

    /**
     * Drops the oldest {@link #PURGE_FRACTION}th. In one pass rather than one entry per insert, so the scan
     * is amortised — and by selecting a threshold rather than sorting, which is the whole map's length again
     * for no gain when all that is needed is "older than most".
     */
    private void purgeLeastRecentlyUsed() {
        int dropCount = Math.max(1, compiled.size() / PURGE_FRACTION);
        List<Long> marks = new ArrayList<>(compiled.size());
        for (Entry entry : compiled.values())
            marks.add(entry.lastUsed);
        marks.sort(null);
        long threshold = marks.get(Math.min(dropCount, marks.size() - 1));
        compiled.entrySet().removeIf(e -> e.getValue().lastUsed <= threshold);
    }

    /**
     * Records a scope this is being asked to compile for, and refuses once there are too many to be a fixed
     * set. Throws rather than returning a verdict because every caller would do the same thing with it.
     */
    void noteScopeToken(Object scopeToken) {
        if (scopeToken == null)
            return;
        String token = scopeToken.toString();
        if (scopeTokens.contains(token))
            return;
        if (scopeTokens.size() >= MAX_SCOPE_TOKENS)
            // The message carries the whole diagnosis and nothing logs it first. A refusal that needs the
            // console to be initialised fails as a ServiceConfigurationError wherever it is not — which is
            // how this check first failed its own test — and a guard whose failure path has a dependency is
            // a guard that works only where it was tried.
            throw new IllegalStateException("Too many distinct data scopes (" + MAX_SCOPE_TOKENS + "):"
                + " a scope token must name one of a FIXED set of scopes. One that varies per caller makes"
                + " the compiled-statement cache grow with the callers. Look at what is being passed as the"
                + " token, not at this cap.");
        scopeTokens.add(token);
    }

    int scopeTokenCount() {
        return scopeTokens.size();
    }

    int size() {
        return compiled.size();
    }

    /** Whether this key is still held — for the checks, which need to see WHICH entries survived a purge. */
    boolean holds(String key) {
        return compiled.containsKey(key);
    }
}
