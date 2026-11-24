package dev.webfx.stack.com.bus;

/**
 * Check for {@link BridgeAddressPolicy}. No test framework: runs from main() and exits non-zero on failure, like the
 * checks in webfx-stack-authz-core.
 */
public class BridgeAddressPolicyCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    public static void main(String[] args) {
        BridgeAddressPolicy.restrictPublishToVerifiedBackoffice("broadcast/observed", false);
        BridgeAddressPolicy.restrictPublishToVerifiedBackoffice("broadcast/enforced", true);
        BridgeAddressPolicy.restrictPublishToServer("agents");
        BridgeAddressPolicy.restrictDeliveryToVerifiedBackoffice("agents");

        System.out.println("addresses nobody registered:");
        check("are not governed", !BridgeAddressPolicy.governs("busCallService") && !BridgeAddressPolicy.governs(null));
        check("any client may publish", BridgeAddressPolicy.publishDecision("busCallService", false) == BridgeAddressPolicy.PublishDecision.ALLOW);
        check("anyone receives", BridgeAddressPolicy.mayDeliver("busCallService", false));

        System.out.println("a back-office broadcast:");
        check("a verified back office publishes", BridgeAddressPolicy.publishDecision("broadcast/enforced", true) == BridgeAddressPolicy.PublishDecision.ALLOW);
        check("anyone else is refused once enforced", BridgeAddressPolicy.publishDecision("broadcast/enforced", false) == BridgeAddressPolicy.PublishDecision.REFUSE);
        check("and only logged while observed", BridgeAddressPolicy.publishDecision("broadcast/observed", false) == BridgeAddressPolicy.PublishDecision.WOULD_REFUSE);
        check("everyone still receives it", BridgeAddressPolicy.mayDeliver("broadcast/enforced", false));

        System.out.println("a server-only, back-office-only address:");
        check("no client publishes, not even a verified back office",
            BridgeAddressPolicy.publishDecision("agents", true) == BridgeAddressPolicy.PublishDecision.REFUSE
            && BridgeAddressPolicy.publishDecision("agents", false) == BridgeAddressPolicy.PublishDecision.REFUSE);
        check("a verified back office receives it", BridgeAddressPolicy.mayDeliver("agents", true));
        check("nobody else does", !BridgeAddressPolicy.mayDeliver("agents", false));

        System.out.println();
        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0)
            System.exit(1);
    }
}
