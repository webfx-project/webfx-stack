package dev.webfx.stack.session.token;

/**
 * The test aid that shortens session lifetimes — and the two properties that make it safe to have at all.
 *
 * <p>A switch that changes how long sessions live is dangerous in exactly one direction. So the cases
 * worth guarding are: it can never LENGTHEN a session, whatever it is given; and it scales every window by
 * the same factor, so what is tested on a developer's machine has the same shape as production. The
 * development-build guard lives in {@code SessionTokenKeysInitializer}, and is checked beside it, by
 * LifetimeScaleGuardCheck in webfx-stack-session-token-plugin.
 *
 * <p>A separate class from RenewalCheck on purpose: the factor is global, and setting it mid-way through
 * another check would silently change every assertion after it.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero while the issue stands.
 */
public class LifetimeScaleCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    static boolean refuses(double factor) {
        try {
            SessionLifetime.setScale(factor);
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    public static void main(String[] args) {
        long access = SessionLifetime.accessWindowMillis();
        long boIdle = SessionLifetime.idleWindowMillis(SessionTier.BACK_OFFICE);
        long boAbsolute = SessionLifetime.absoluteLifetimeMillis(SessionTier.BACK_OFFICE);

        System.out.println("unscaled — what every real deployment runs:");
        check("the factor is 1", SessionLifetime.getScale() == 1.0);
        check("the access window is thirty minutes", access == 30 * 60 * 1000L);
        check("a back-office session lasts a day", boAbsolute == 24 * 3600 * 1000L);

        System.out.println("it can NEVER lengthen a session, whatever it is given:");
        check("a factor above 1 is refused", refuses(2.0));
        check("so is zero", refuses(0));
        check("so is a negative", refuses(-0.5));
        check("so is not-a-number", refuses(Double.NaN));
        check("and a refusal leaves the lifetimes untouched", SessionLifetime.accessWindowMillis() == access);

        System.out.println("it shortens every window by the same factor:");
        SessionLifetime.setScale(0.5);
        check("the access window halves", SessionLifetime.accessWindowMillis() == access / 2);
        check("the idle window halves", SessionLifetime.idleWindowMillis(SessionTier.BACK_OFFICE) == boIdle / 2);
        check("the absolute bound halves", SessionLifetime.absoluteLifetimeMillis(SessionTier.BACK_OFFICE) == boAbsolute / 2);
        // The ratios are most of what is worth testing, so they must survive the scaling intact.
        check("so the idle/absolute ratio is exactly production's",
              SessionLifetime.absoluteLifetimeMillis(SessionTier.BACK_OFFICE) * boIdle
              == SessionLifetime.idleWindowMillis(SessionTier.BACK_OFFICE) * boAbsolute);

        System.out.println("too small is clamped rather than obeyed:");
        double applied = SessionLifetime.setScale(0.0001);
        check("clamped to the minimum", applied == SessionLifetime.MINIMUM_SCALE);
        check("so the access window stays usable (at least 9 seconds)", SessionLifetime.accessWindowMillis() >= 9_000);

        System.out.println("a token minted under a scale carries the scaled bounds:");
        SessionLifetime.setScale(0.5);
        long now = 1_700_000_000_000L;
        check("its access window is the scaled one",
              SessionLifetime.accessExpiryFrom(now) == now + access / 2);
        check("renewal falls due in the last quarter of the SCALED window",
              SessionLifetime.isRenewalDue(now + access / 2 - access / 8, now + access / 2));
        // The point that tells the two apart: inside the last quarter of the UNSCALED window, but not yet
        // inside the last quarter of the scaled one. Without it, a renewal rule that ignored the scale
        // would pass the check above just the same.
        check("and not before it, though an unscaled rule would already call it due",
              !SessionLifetime.isRenewalDue(now + access / 2 - 3 * access / 16, now + access / 2));

        SessionLifetime.setScale(1.0);
        check("and it resets cleanly", SessionLifetime.accessWindowMillis() == access);

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
