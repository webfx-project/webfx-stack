package dev.webfx.stack.authz.server;

import dev.webfx.extras.operation.HasOperationCode;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.authz.core.HasAuthorizationContext;
import dev.webfx.stack.authz.core.InMemoryAuthorizationRuleRegistry;
import dev.webfx.stack.authz.core.operation.OperationAuthorizationRuleParser;
import dev.webfx.stack.authz.server.spi.AuthorizationServerServiceProvider;
import dev.webfx.stack.authz.server.spi.impl.AuthorizationServerServiceProviderBase;

import java.util.Map;

/**
 * {@link AuthorizationCheck} through the base provider: the "held somewhere" question, and the fresh one.
 *
 * <p>The provider here serves grants from a string the check rewrites, and counts how often it is asked for them:
 * that is what shows a fresh check reading the grants as they are NOW while the cached path keeps the old answer,
 * and that a fresh check leaves the cache alone.
 *
 * <p>No test framework — runs from main() and exits non-zero, like the core module's checks.
 *
 * @author Bruno Salmon
 */
public class AuthorizationCheckSemanticsCheck {

    static int pass = 0, fail = 0;

    record Op(String code, Map<String, String> context) implements HasOperationCode, HasAuthorizationContext {
        Op(String code) { this(code, null); }
        public Object getOperationCode() { return code; }
        public Map<String, String> getAuthorizationContext() { return context; }
    }

    static final class GrantsProvider extends AuthorizationServerServiceProviderBase {
        String grants = "";
        int cachedBuilds = 0, freshBuilds = 0;

        @Override public Future<Void> pushAuthorizations() { return Future.succeededFuture(); }

        @Override protected Future<InMemoryAuthorizationRuleRegistry> createUserRuleRegistry(Object userId, boolean backoffice) {
            cachedBuilds++;
            return Future.succeededFuture(parse(grants));
        }

        @Override protected Future<InMemoryAuthorizationRuleRegistry> createFreshUserRuleRegistry(Object userId, boolean backoffice) {
            freshBuilds++;
            return Future.succeededFuture(parse(grants));
        }

        static InMemoryAuthorizationRuleRegistry parse(String grants) {
            InMemoryAuthorizationRuleRegistry registry = new InMemoryAuthorizationRuleRegistry();
            registry.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
            for (String line : grants.split("\n"))
                if (!line.isBlank())
                    registry.registerAuthorizationRule(line.trim());
            return registry;
        }
    }

    public static void main(String[] args) {
        AuthorizationCheck wholeOrganization = AuthorizationCheck.inSomeContextKeyedBy("organizationId");

        System.out.println("held in some context:");
        GrantsProvider p = new GrantsProvider();
        p.grants = """
            context:organizationId=5
            grant operation:DataProtection
            context:organizationId=7,eventId=1857
            grant operation:EditLetter
            """;
        check("an org-wide grant answers 'held for a whole organization'",
              answer(p.isAuthorized(new Op("DataProtection"), wholeOrganization)), true);
        check("the request's own context is not needed for it",
              answer(p.isAuthorized(new Op("DataProtection", Map.of("organizationId", "99")), wholeOrganization)), true);
        check("...whereas the default check judges that context, and refuses",
              answer(p.isAuthorized(new Op("DataProtection", Map.of("organizationId", "99")))), false);
        check("an event-scoped grant is not held for a whole organization",
              answer(p.isAuthorized(new Op("EditLetter"), wholeOrganization)), false);
        check("...but is, keyed by organization AND event",
              answer(p.isAuthorized(new Op("EditLetter"), AuthorizationCheck.inSomeContextKeyedBy("organizationId", "eventId"))), true);

        System.out.println("fresh:");
        GrantsProvider f = new GrantsProvider();
        f.grants = "context:organizationId=5\ngrant operation:DataProtection\n";
        check("held before the revocation (cached)", answer(f.isAuthorized(new Op("DataProtection"), wholeOrganization)), true);
        f.grants = ""; // the role is revoked; the cached registry does not know yet
        check("the cached answer is still the old one",
              answer(f.isAuthorized(new Op("DataProtection"), wholeOrganization)), true);
        check("a fresh check sees the revocation at once",
              answer(f.isAuthorized(new Op("DataProtection"), wholeOrganization.fresh())), false);
        check("a fresh check built its own registry, once", f.freshBuilds == 1, true);
        check("...and left the cache as it was: one cached build in all", f.cachedBuilds == 1, true);

        System.out.println("a provider that does not implement a check refuses it:");
        AuthorizationServerServiceProvider allowAll = new AuthorizationServerServiceProvider() {
            @Override public Future<Void> pushAuthorizations() { return Future.succeededFuture(); }
            @Override public Future<Boolean> isAuthorized(Object request) { return Future.succeededFuture(true); }
        };
        check("the default check is passed through", answer(allowAll.isAuthorized(new Op("X"), AuthorizationCheck.DEFAULT)), true);
        check("...and so is a null check", answer(allowAll.isAuthorized(new Op("X"), null)), true);
        check("'held somewhere' is refused", answer(allowAll.isAuthorized(new Op("X"), wholeOrganization)), false);
        check("'fresh' is refused", answer(allowAll.isAuthorized(new Op("X"), AuthorizationCheck.DEFAULT.fresh())), false);

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }

    /** These providers answer synchronously; an incomplete or failed future is a refusal. */
    static boolean answer(Future<Boolean> future) {
        return future.isComplete() && future.succeeded() && Boolean.TRUE.equals(future.result());
    }

    static void check(String what, boolean actual, boolean expected) {
        if (actual == expected) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what + " (got " + actual + ", wanted " + expected + ")"); }
    }
}
