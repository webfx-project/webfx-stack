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

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
