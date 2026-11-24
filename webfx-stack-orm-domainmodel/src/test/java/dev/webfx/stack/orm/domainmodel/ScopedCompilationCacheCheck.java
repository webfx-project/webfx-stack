package dev.webfx.stack.orm.domainmodel;

import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlCompiled;

import java.util.Collections;

/**
 * That the compiled-statement cache is bounded in both of its dimensions, and that the two are bounded
 * differently on purpose.
 *
 * <p>Statements are keyed by text a client chooses, so the cap there protects the server from traffic and
 * clearing is an acceptable cost. Scope tokens can only be chosen by our own code, so the cap there is
 * diagnostic: it exists to make "somebody passed caller-derived data as a token" fail at once rather than
 * appear later as memory that will not come back.
 */
public class ScopedCompilationCacheCheck {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        ScopedCompilationCache cache = new ScopedCompilationCache();

        check("an unscoped key is the statement itself",
              "select 1".equals(ScopedCompilationCache.keyOf("select 1", null)));
        check("a scoped key separates the token from the text",
              !ScopedCompilationCache.keyOf("select 1", "scopeA")
                  .equals(ScopedCompilationCache.keyOf("select 1", "scopeB")));
        check("two scopes do not share one entry",
              !ScopedCompilationCache.keyOf("select 1", "scopeA").equals("select 1"));

        // Statements: bounded against traffic, by dropping the least recently used.
        cache.put("the application's own statement", compiled());
        for (int i = 0; i <= ScopedCompilationCache.MAX_COMPILED_STATEMENTS; i++) {
            cache.put("minted variant " + i, compiled());
            cache.get("the application's own statement"); // in constant use, as a real statement is
        }
        check("the statement cache is bounded rather than growing without limit",
              cache.size() <= ScopedCompilationCache.MAX_COMPILED_STATEMENTS);
        // The point of evicting by use rather than clearing: a caller minting statements must not cost the
        // application the entries it depends on. Under clear-all this one was discarded with the noise.
        check("a statement in constant use survives a flood of minted ones",
              cache.holds("the application's own statement"));
        check("and the flood's own oldest entries are the ones gone",
              !cache.holds("minted variant 0"));

        // Scope tokens: no token at all is the case every statement is in today, and must cost nothing.
        for (int i = 0; i < 1000; i++)
            cache.noteScopeToken(null);
        check("an unscoped compilation records no token", cache.scopeTokenCount() == 0);

        // A fixed set of named scopes is accepted.
        for (int i = 0; i < ScopedCompilationCache.MAX_SCOPE_TOKENS; i++)
            cache.noteScopeToken("scope" + i);
        check("a fixed set of scopes is accepted",
              cache.scopeTokenCount() == ScopedCompilationCache.MAX_SCOPE_TOKENS);
        check("and a scope already seen is free",
              noThrow(() -> cache.noteScopeToken("scope0")));

        // One too many means the token is not naming a scope. Refuse, loudly, rather than leak.
        check("a token that keeps varying is refused, not cached",
              throwsIllegalState(() -> cache.noteScopeToken("perCallerValue")));

        System.out.println(fail == 0 ? "\nALL " + pass + " PASS" : "\n" + fail + " FAILED");
        if (fail > 0)
            throw new AssertionError(fail + " cache bound checks failed");
    }

    private static SqlCompiled compiled() {
        return new SqlCompiled("select 1", null, Collections.emptyList(), true, null, null, null, true);
    }

    private static boolean noThrow(Runnable r) {
        try { r.run(); return true; } catch (Throwable t) { return false; }
    }

    private static boolean throwsIllegalState(Runnable r) {
        try { r.run(); return false; } catch (IllegalStateException e) { return true; } catch (Throwable t) { return false; }
    }

    private static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else    { fail++; System.out.println("  FAIL " + what); }
    }
}
