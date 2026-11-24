import dev.webfx.extras.operation.HasOperationCode;
import dev.webfx.stack.authz.core.InMemoryAuthorizationRuleRegistry;
import dev.webfx.stack.authz.core.operation.OperationAuthorizationRuleParser;
import java.util.HashMap;
import java.util.Map;

/**
 * Semantics check for the rule engine, deliberately written with no test framework.
 *
 * <p>This repository declares no JUnit: the dependency exists in modality-fork's webfx.xml but is
 * commented out as "time-consuming for the WebFX CLI", so adding it here would slow every
 * `webfx update` for everyone. Rather than impose that, or ship an engine with nothing checking it,
 * this runs from main():
 *
 * <pre>
 * mvn -q install -DskipTests -pl webfx-stack-fork/webfx-stack-authz-core
 * java -cp "webfx-stack-fork/webfx-stack-authz-core/target/classes:$(deps)" \
 *      dev.webfx.stack.authz.core.CoreSemanticsCheck   # exits non-zero on failure
 * </pre>
 *
 * <p>It covers what the extraction could plausibly have broken: that a global grant ignores context,
 * that a scoped grant applies ONLY in its own context (including the null and empty cases), that every
 * key of a multi-key context must match, that the wildcard still excludes RouteTo codes, and that
 * revoke still beats grant.
 *
 * <p>The case worth keeping is "two callers, two contexts, one registry". Before the context became a
 * parameter it could not even be expressed: the engine read one ambient map per process, which is
 * right for a single user driving a single screen and wrong for a server answering many callers at
 * once. That test passing is what says this engine can move server-side.
 */
public class CoreSemanticsCheck {
    record Op(String code) implements HasOperationCode {
        public Object getOperationCode() { return code; }
    }
    static int passed = 0, failed = 0;
    static void check(String what, boolean actual, boolean expected) {
        if (actual == expected) { passed++; System.out.println("  ok   " + what); }
        else { failed++; System.out.println("  FAIL " + what + " (got " + actual + ", wanted " + expected + ")"); }
    }
    static Map<String,String> ctx(String... kv) {
        Map<String,String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i+1]);
        return m;
    }
    public static void main(String[] args) {
        InMemoryAuthorizationRuleRegistry r = new InMemoryAuthorizationRuleRegistry();
        r.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());

        // A global grant, then an org-scoped one, then a super-admin section.
        r.registerAuthorizationRule("grant operation:GuestOp");
        r.registerAuthorizationRule("context:organizationId=42");
        r.registerAuthorizationRule("grant operation:EditLetter");
        r.registerAuthorizationRule("context:organizationId=7,eventId=1857");
        r.registerAuthorizationRule("grant operation:TranslateEvent");

        System.out.println("global grant ignores context:");
        check("GuestOp with no context",      r.doesRulesAuthorize(new Op("GuestOp"), ctx()), true);
        check("GuestOp in org 42",            r.doesRulesAuthorize(new Op("GuestOp"), ctx("organizationId","42")), true);
        check("UnknownOp refused",            r.doesRulesAuthorize(new Op("UnknownOp"), ctx()), false);

        System.out.println("org-scoped grant applies ONLY in its context:");
        check("EditLetter in org 42",         r.doesRulesAuthorize(new Op("EditLetter"), ctx("organizationId","42")), true);
        check("EditLetter in org 7",          r.doesRulesAuthorize(new Op("EditLetter"), ctx("organizationId","7")), false);
        check("EditLetter with no context",   r.doesRulesAuthorize(new Op("EditLetter"), ctx()), false);
        check("EditLetter with null context", r.doesRulesAuthorize(new Op("EditLetter"), null), false);

        System.out.println("multi-key context must match on every key:");
        check("TranslateEvent org7+event1857", r.doesRulesAuthorize(new Op("TranslateEvent"), ctx("organizationId","7","eventId","1857")), true);
        check("TranslateEvent org7 only",      r.doesRulesAuthorize(new Op("TranslateEvent"), ctx("organizationId","7")), false);
        check("TranslateEvent wrong event",    r.doesRulesAuthorize(new Op("TranslateEvent"), ctx("organizationId","7","eventId","1")), false);

        System.out.println("two callers, two contexts, one registry (the server case):");
        check("caller A sees org-42 grant",    r.doesRulesAuthorize(new Op("EditLetter"), ctx("organizationId","42")), true);
        check("caller B does not",             r.doesRulesAuthorize(new Op("EditLetter"), ctx("organizationId","99")), false);

        System.out.println("super-admin wildcard, and its RouteTo exclusion:");
        InMemoryAuthorizationRuleRegistry su = new InMemoryAuthorizationRuleRegistry();
        su.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
        su.registerAuthorizationRule("context:any");
        su.registerAuthorizationRule("grant operation:*");
        check("* grants a plain operation",    su.doesRulesAuthorize(new Op("AnythingAtAll"), ctx()), true);
        check("* excludes RouteTo codes",      su.doesRulesAuthorize(new Op("RouteToKitchen"), ctx()), false);
        check("context:any ignores context",   su.doesRulesAuthorize(new Op("AnythingAtAll"), ctx("organizationId","1")), true);

        System.out.println("revoke wins over grant:");
        InMemoryAuthorizationRuleRegistry rv = new InMemoryAuthorizationRuleRegistry();
        rv.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
        rv.registerAuthorizationRule("grant operation:Dangerous");
        rv.registerAuthorizationRule("revoke operation:Dangerous");
        check("granted then revoked",          rv.doesRulesAuthorize(new Op("Dangerous"), ctx()), false);
        // Across contexts: a context-free revoke and an org-scoped grant. Several organizations, so that whatever
        // order the map visits its contexts in, at least one grant section is visited after the revoke.
        InMemoryAuthorizationRuleRegistry rx = new InMemoryAuthorizationRuleRegistry();
        rx.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
        rx.registerAuthorizationRule("revoke operation:Dangerous");
        for (int org = 1; org <= 20; org++) {
            rx.registerAuthorizationRule("context:organizationId=" + org);
            rx.registerAuthorizationRule("grant operation:Dangerous");
        }
        boolean refusedEverywhere = true;
        for (int org = 1; org <= 20; org++)
            refusedEverywhere &= !rx.doesRulesAuthorize(new Op("Dangerous"), ctx("organizationId", String.valueOf(org)));
        check("a context-free revoke beats a grant in any organization", refusedEverywhere, true);
        check("...and 'held somewhere' too",
              rx.doesRulesAuthorizeInSomeContext(new Op("Dangerous"), c -> c.size() == 1 && c.containsKey("organizationId")), false);
        // The other way round, the one the shared result actually broke: a context-free grant (a super administrator's
        // wildcard) and a revoke in organization sections. The revoke must win in those organizations, and only there.
        InMemoryAuthorizationRuleRegistry ry = new InMemoryAuthorizationRuleRegistry();
        ry.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
        ry.registerAuthorizationRule("context:any");
        ry.registerAuthorizationRule("grant operation:*");
        for (int org = 1; org <= 20; org++) {
            ry.registerAuthorizationRule("context:organizationId=" + org);
            ry.registerAuthorizationRule("revoke operation:Dangerous");
        }
        boolean revokedEverywhere = true;
        for (int org = 1; org <= 20; org++)
            revokedEverywhere &= !ry.doesRulesAuthorize(new Op("Dangerous"), ctx("organizationId", String.valueOf(org)));
        check("an org-scoped revoke beats a context-free grant there",  revokedEverywhere, true);
        check("...but not elsewhere",                                   ry.doesRulesAuthorize(new Op("Dangerous"), ctx("organizationId", "99")), true);
        check("...and other operations stay granted in those orgs",     ry.doesRulesAuthorize(new Op("Other"), ctx("organizationId", "3")), true);

        // V0166: an explicit-only operation is not covered by a wildcard, only by a grant naming it. The first check
        // is the one the change exists for; delete the skip in computeRuleResult and it fails.
        System.out.println("explicit-only operations (V0166):");
        InMemoryAuthorizationRuleRegistry ex = new InMemoryAuthorizationRuleRegistry();
        ex.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
        ex.registerAuthorizationRule("context:any");
        ex.registerAuthorizationRule("grant operation:*");
        // Declared AFTER the wildcard, and inside a context: neither position may matter
        ex.registerAuthorizationRule("explicit operation:DataProtection");
        check("* does not grant an explicit-only op",  ex.doesRulesAuthorize(new Op("DataProtection"), ctx()), false);
        check("* does not grant it in any context",    ex.doesRulesAuthorize(new Op("DataProtection"), ctx("organizationId","1")), false);
        check("* still grants every other op",         ex.doesRulesAuthorize(new Op("AnythingAtAll"), ctx()), true);
        ex.registerAuthorizationRule("context:organizationId=5");
        ex.registerAuthorizationRule("grant operation:DataProtection");
        check("a grant naming it still grants it",     ex.doesRulesAuthorize(new Op("DataProtection"), ctx("organizationId","5")), true);
        check("...only in its own context",            ex.doesRulesAuthorize(new Op("DataProtection"), ctx("organizationId","6")), false);
        ex.clearAllAuthorizationRules();
        ex.registerAuthorizationRule("context:any");
        ex.registerAuthorizationRule("grant operation:*");
        check("clearing rules clears the declaration", ex.doesRulesAuthorize(new Op("DataProtection"), ctx()), true);

        InMemoryAuthorizationRuleRegistry exRevoke = new InMemoryAuthorizationRuleRegistry();
        exRevoke.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
        exRevoke.registerAuthorizationRule("explicit operation:DataProtection");
        exRevoke.registerAuthorizationRule("grant operation:DataProtection");
        exRevoke.registerAuthorizationRule("revoke operation:*");
        check("a wildcard revoke still refuses it",    exRevoke.doesRulesAuthorize(new Op("DataProtection"), ctx()), false);

        // "Held for a whole organization, in some organization": the question with no target to take a context from.
        System.out.println("in some context (held anywhere):");
        java.util.function.Predicate<Map<String,String>> wholeOrg = c -> c.size() == 1 && c.containsKey("organizationId");
        InMemoryAuthorizationRuleRegistry any = new InMemoryAuthorizationRuleRegistry();
        any.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
        any.registerAuthorizationRule("grant operation:GuestOp");
        any.registerAuthorizationRule("context:organizationId=5");
        any.registerAuthorizationRule("grant operation:DataProtection");
        any.registerAuthorizationRule("context:organizationId=7,eventId=1857");
        any.registerAuthorizationRule("grant operation:EditLetter");
        any.registerAuthorizationRule("context:organizationId=8");
        any.registerAuthorizationRule("grant operation:Revoked");
        any.registerAuthorizationRule("revoke operation:Revoked");
        check("an org-wide grant counts",               any.doesRulesAuthorizeInSomeContext(new Op("DataProtection"), wholeOrg), true);
        check("a context-free grant counts",            any.doesRulesAuthorizeInSomeContext(new Op("GuestOp"), wholeOrg), true);
        check("an event-scoped grant does not",         any.doesRulesAuthorizeInSomeContext(new Op("EditLetter"), wholeOrg), false);
        check("...unless the filter accepts its context", any.doesRulesAuthorizeInSomeContext(new Op("EditLetter"), c -> true), true);
        check("a grant revoked in its own context does not", any.doesRulesAuthorizeInSomeContext(new Op("Revoked"), wholeOrg), false);
        check("an ungranted operation does not",        any.doesRulesAuthorizeInSomeContext(new Op("Nothing"), wholeOrg), false);
        check("the filter is the gate: none accepted",  any.doesRulesAuthorizeInSomeContext(new Op("DataProtection"), c -> false), false);

        // The DPO question for a super administrator: the wildcard is context-free, but does not cover DataProtection.
        InMemoryAuthorizationRuleRegistry suAny = new InMemoryAuthorizationRuleRegistry();
        suAny.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
        suAny.registerAuthorizationRule("explicit operation:DataProtection");
        suAny.registerAuthorizationRule("context:any");
        suAny.registerAuthorizationRule("grant operation:*");
        check("a super admin's wildcard is not DataProtection anywhere", suAny.doesRulesAuthorizeInSomeContext(new Op("DataProtection"), wholeOrg), false);
        check("...but covers any other operation anywhere",             suAny.doesRulesAuthorizeInSomeContext(new Op("EditBooking"), wholeOrg), true);
        suAny.registerAuthorizationRule("context:organizationId=5");
        suAny.registerAuthorizationRule("grant operation:DataProtection");
        check("a super admin holding the role is",                       suAny.doesRulesAuthorizeInSomeContext(new Op("DataProtection"), wholeOrg), true);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
