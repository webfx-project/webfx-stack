package dev.webfx.stack.db.query;

import dev.webfx.platform.console.spi.ConsoleProvider;
import dev.webfx.platform.service.SingleServiceProvider;

/**
 * That a data scope reaches the compiler, reaches only a client's own query, and never reaches the wire.
 *
 * <p>Four properties, each of which was a way to get this wrong.
 *
 * <p><b>A server read is not a client read.</b> The token is set at the three generic query endpoints and
 * nowhere else, so an endpoint's own lookup — which reaches the query service directly — compiles unscoped.
 * Deciding it from the thread's origin instead would have scoped those too, and the first casualty would have
 * been an authorization lookup reading through the filter it exists to check, which is not a check. The
 * negative case below is the one to keep: an argument built the way server code builds one carries no token.
 *
 * <p><b>Two scopes must not share a subscription.</b> A push subscription is keyed by its argument, and the
 * same statement compiles to different sql under different scopes — so if the token sat outside equality they
 * would compare equal, share one QueryInfo, and the second subscriber would be served rows fetched under the
 * first's scope. Fragmentation is by scope, not by caller: the fixed-set rule on tokens is what bounds it.
 *
 * <p><b>A client cannot supply one.</b> The field is transient and the serial codec names every field it
 * encodes, so it is off the wire by construction rather than by a refusal someone has to remember to write.
 *
 * <p><b>No scope is not a licence to read everything.</b> While nothing is enforced, an unscoped compile is
 * precisely what every query did before scopes existed, so a resolver that fails must not take the read down
 * with it — it would refuse reads to protect nothing. The moment a root is enforced that reverses: the scope
 * condition is the only thing between the caller and every centre's rows, and {@code ClientReadScope}
 * injects nothing at all for a null token. The last cases below pin both halves and the residual between
 * them, because reading those two states as one is how this was wrong until 2026-11-18.
 *
 * @author Claude Code
 */
public class ClientScopeTokenCheck {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        SingleServiceProvider.registerServiceProvider(ConsoleProvider.class, new ConsoleProvider() {
            @Override public void log(String message) { }
            @Override public void error(String message, Throwable error) { }
        });

        // ── Nothing registered: every query compiles exactly as it did before scopes existed ──────────────
        check("with no resolver there is no scope", ClientScopeToken.forCaller() == null);
        QueryArgument plain = argument("select id from Document d where event=$1");
        check("and an argument carries none", plain.getScopeToken() == null);

        // ── THE NEGATIVE ONE. Server code builds its arguments directly; only the endpoints set a token. ──
        ClientScopeToken.registerResolver(() -> "member");
        check("a resolver answers for a client caller", "member".equals(ClientScopeToken.forCaller()));
        check("but an argument built directly - as an endpoint's own lookup does - is still unscoped",
              argument("select viewerPerson from Conversation where id=$1").getScopeToken() == null);

        // ── Two scopes, one statement ─────────────────────────────────────────────────────────────────────
        QueryArgument asMember = scoped(plain, "member");
        QueryArgument asBackoffice = scoped(plain, "backoffice");
        check("the token reaches the argument", "member".equals(asMember.getScopeToken()));
        check("two scopes are NOT equal arguments, so they cannot share a subscription",
              !asMember.equals(asBackoffice));
        check("and do not collide in a hash map, which is how subscriptions are keyed",
              asMember.hashCode() != asBackoffice.hashCode());
        check("the same scope twice IS equal, so callers under one scope still share",
              scoped(plain, "member").equals(asMember));
        check("and an unscoped argument is not equal to a scoped one",
              !plain.equals(asMember));

        // ── copy() must carry it, or the interceptor's rebuild would silently unscope the query ───────────
        check("copy carries the token",
              "member".equals(QueryArgument.builder().copy(asMember).build().getScopeToken()));
        check("copy still carries it when the statement is rewritten, which is what the interceptor does",
              "member".equals(QueryArgument.builder().copy(asMember)
                  .setStatement("select id from document as d").build().getScopeToken()));

        // ── NOTHING ENFORCED: a resolver that throws must not fail the query ─────────────────────────────
        ClientScopeToken.registerResolver(() -> { throw new IllegalStateException("boom"); });
        check("with no scope required, a resolver that throws compiles unscoped rather than failing the read",
              ClientScopeToken.forCaller() == null);

        // ── A ROOT ENFORCED: the same two answers would hand over every centre's rows, so they refuse ────
        ClientScopeToken.registerResolver(required(() -> { throw new IllegalStateException("boom"); }));
        check("with a scope required, a resolver that throws REFUSES instead of compiling unscoped",
              ClientScopeToken.SCOPE_UNAVAILABLE_REFUSED.equals(refusal()));

        ClientScopeToken.registerResolver(required(() -> null));
        check("with a scope required, no token refuses too - a null one injects no condition at all",
              ClientScopeToken.SCOPE_UNAVAILABLE_REFUSED.equals(refusal()));

        ClientScopeToken.registerResolver(required(() -> "backoffice"));
        check("but a caller whose scope IS resolved reads under it, refusing nothing",
              "backoffice".equals(ClientScopeToken.forCaller()));
        check("and that read is not a refusal", refusal() == null);

        // ── THE RESIDUAL, pinned so it stays a decision rather than becoming an accident ──────────────────
        ClientScopeToken.registerResolver(new ClientScopeToken.Resolver() {
            @Override public Object tokenForCaller() { return null; }
            @Override public boolean isScopeRequired() { throw new IllegalStateException("boom"); }
        });
        check("a resolver that cannot even say whether a scope is required compiles unscoped, rather than "
              + "refusing every client read on the server at once",
              ClientScopeToken.forCaller() == null);

        ClientScopeToken.registerResolver(null);
        check("and unregistering returns to unscoped", ClientScopeToken.forCaller() == null);

        System.out.println(fail == 0 ? "\nALL " + pass + " PASS" : "\n" + fail + " FAILED");
        if (fail > 0)
            throw new AssertionError(fail + " scope token checks failed");
    }

    /** The same resolver as it behaves once a root is enforced: a scope is then required. */
    private static ClientScopeToken.Resolver required(ClientScopeToken.Resolver r) {
        return new ClientScopeToken.Resolver() {
            @Override public Object tokenForCaller() { return r.tokenForCaller(); }
            @Override public boolean isScopeRequired() { return true; }
        };
    }

    /** What forCaller() refused with, or null when it answered with a token. */
    private static String refusal() {
        try {
            ClientScopeToken.forCaller();
            return null;
        } catch (ClientScopeToken.ScopeUnavailableException e) {
            return e.getMessage();
        }
    }

    private static QueryArgument scoped(QueryArgument argument, Object token) {
        return QueryArgument.builder().copy(argument).setScopeToken(token).build();
    }

    private static QueryArgument argument(String statement) {
        return QueryArgument.builder().setDataSourceId("ds").setLanguage("DQL")
                .setStatement(statement).setParameters(7).build();
    }

    private static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else    { fail++; System.out.println("  FAIL " + what); }
    }
}
