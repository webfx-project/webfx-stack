package dev.webfx.stack.session.token;

/**
 * Check for {@link IdentityTokenPolicy} — the flip, and the one decision here not made by cryptography.
 *
 * <p>No test framework: this repository declares no JUnit (modality-fork has it commented out as
 * time-consuming for the WebFX CLI), so this runs from main() and exits non-zero on failure.
 *
 * <p>The rule is two booleans, which is exactly why it is worth pinning. Both ways of getting it wrong
 * are quiet. Refusing too little leaves the hole open while every log line says the flip is on. Refusing
 * too much strips claims that were never made — logging out anonymous visitors, and doing it on every
 * message rather than once, so the first symptom is a log filling up rather than anyone reporting it.
 */
public class IdentityTokenPolicyCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); }
        else { fail++; System.out.println("  FAIL " + what); }
    }

    public static void main(String[] args) {
        System.out.println("default:");
        check("off unless something turns it on", !IdentityTokenPolicy.isTokenRequired());
        check("a real claim stands while off", !IdentityTokenPolicy.refusesUntokenedClaim(true));

        System.out.println("off — the migration state, in which nothing may change:");
        IdentityTokenPolicy.setTokenRequired(false);
        check("real identity claimed, no token: honoured", !IdentityTokenPolicy.refusesUntokenedClaim(true));
        check("nothing claimed: untouched", !IdentityTokenPolicy.refusesUntokenedClaim(false));

        System.out.println("on — the flip:");
        IdentityTokenPolicy.setTokenRequired(true);
        check("real identity claimed, no token: REFUSED", IdentityTokenPolicy.refusesUntokenedClaim(true));
        // The two that must survive the flip untouched. An anonymous front-office visitor browsing a public
        // page claims nothing, and a client that has just logged out claims the logged-out sentinel; neither
        // is asserting an identity, so neither has anything to prove.
        check("anonymous visitor (no user id): untouched", !IdentityTokenPolicy.refusesUntokenedClaim(false));
        check("logged-out client re-communicating logout: untouched", !IdentityTokenPolicy.refusesUntokenedClaim(false));

        System.out.println("reversibility — the property that makes this safe to turn on at all:");
        IdentityTokenPolicy.setTokenRequired(false);
        check("turning it back off restores the previous behaviour exactly",
            !IdentityTokenPolicy.refusesUntokenedClaim(true) && !IdentityTokenPolicy.isTokenRequired());

        System.out.println(pass + " passed, " + fail + " failed");
        if (fail > 0)
            System.exit(1);
    }
}
