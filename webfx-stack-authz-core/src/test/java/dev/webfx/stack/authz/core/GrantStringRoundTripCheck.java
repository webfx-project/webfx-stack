import dev.webfx.extras.operation.HasOperationCode;
import dev.webfx.stack.authz.core.InMemoryAuthorizationRuleRegistry;
import dev.webfx.stack.authz.core.operation.OperationAuthorizationRuleParser;
import java.util.HashMap;
import java.util.Map;

/**
 * Feeds a grant string of the shape the server pushes through the parse the server performs.
 *
 * <p>Companion to {@link CoreSemanticsCheck}, which tests the engine against rules built directly.
 * This one starts from the wire format — the newline-delimited text
 * ModalityAuthorizationServerServiceProvider emits and both tiers parse — because that string is the
 * contract between them, and a grammar change that silently stopped granting would show up nowhere
 * else. Run it the same way, from main().
 *
 * <p>The negative cases carry the weight. A `grant route:/letters` line must not end up granting an
 * operation, and it must not grant `RouteToLetters` either: the server registers only the operation
 * parser, so route lines are dropped rather than reinterpreted. If they ever were interpreted, a user
 * with permission to open a screen would silently gain permission to perform whatever the server
 * decided that screen implied.
 */
public class GrantStringRoundTripCheck {
    record Op(String code) implements HasOperationCode { public Object getOperationCode() { return code; } }
    static int pass=0, fail=0;
    static void check(String w, boolean a, boolean e) {
        if (a==e) { pass++; System.out.println("  ok   "+w); } else { fail++; System.out.println("  FAIL "+w); }
    }
    static Map<String,String> ctx(String... kv){ Map<String,String> m=new HashMap<>();
        for(int i=0;i<kv.length;i+=2) m.put(kv[i],kv[i+1]); return m; }

    // Exactly the grammar ModalityAuthorizationServerServiceProvider emits.
    static final String GRANTS = String.join("\n",
        "grant operation:GuestOp",
        "grant route:/public/*",
        "context:organizationId=42",
        "grant operation:EditLetterContent",
        "grant route:/letters",
        "context:organizationId=42,eventId=1857",
        "grant operation:TranslateEvent");

    public static void main(String[] a) {
        // The exact parse the server provider performs.
        InMemoryAuthorizationRuleRegistry r = new InMemoryAuthorizationRuleRegistry();
        r.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
        for (String line : GRANTS.split("\n")) { line = line.trim(); if (!line.isEmpty()) r.registerAuthorizationRule(line); }

        System.out.println("operations survive the round trip:");
        check("GuestOp granted globally", r.doesRulesAuthorize(new Op("GuestOp"), ctx()), true);
        check("EditLetterContent in org 42", r.doesRulesAuthorize(new Op("EditLetterContent"), ctx("organizationId","42")), true);
        check("EditLetterContent in org 7 refused", r.doesRulesAuthorize(new Op("EditLetterContent"), ctx("organizationId","7")), false);
        check("TranslateEvent needs both keys", r.doesRulesAuthorize(new Op("TranslateEvent"), ctx("organizationId","42","eventId","1857")), true);
        check("TranslateEvent org-only refused", r.doesRulesAuthorize(new Op("TranslateEvent"), ctx("organizationId","42")), false);
        check("ungranted operation refused", r.doesRulesAuthorize(new Op("DeleteEverything"), ctx("organizationId","42")), false);

        System.out.println("route lines are ignored server-side, not mistaken for operations:");
        check("route line did not grant an op named /letters", r.doesRulesAuthorize(new Op("/letters"), ctx("organizationId","42")), false);
        check("RouteToLetters not granted by a route line", r.doesRulesAuthorize(new Op("RouteToLetters"), ctx("organizationId","42")), false);

        System.out.println("the logged-out grant set:");
        InMemoryAuthorizationRuleRegistry out = new InMemoryAuthorizationRuleRegistry();
        out.setAuthorizationRuleParser(new OperationAuthorizationRuleParser());
        for (String line : "logout\ngrant operation:PublicOp".split("\n")) out.registerAuthorizationRule(line.trim());
        check("public op granted", out.doesRulesAuthorize(new Op("PublicOp"), ctx()), true);
        check("staff op refused", out.doesRulesAuthorize(new Op("EditLetterContent"), ctx()), false);

        System.out.println("\n"+pass+" passed, "+fail+" failed");
        if (fail>0) System.exit(1);
    }
}
