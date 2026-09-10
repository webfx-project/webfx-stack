package dev.webfx.stack.session.token.plugin;

import dev.webfx.stack.session.token.SessionLifetime;

/**
 * The lifetime scale is refused by any build that is not a development build — the property that makes it
 * safe to leave a test aid in the code at all.
 *
 * <p>SessionLifetime refuses to LENGTHEN a session whoever asks, and LifetimeScaleCheck covers that. What
 * only this can cover is the guard in front of it: that a staging or production build ignores the setting
 * however it is configured. It is the line most exposed to a well-meant edit — inverted, dropped, or moved
 * below the call it protects — and nothing else would notice, because the rest of the system works
 * perfectly well with sessions that are merely short.
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core. Run from main(); it exits
 * non-zero while the issue stands.
 */
public class LifetimeScaleGuardCheck {

    static int pass = 0, fail = 0;

    static void check(String what, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + what); } else { fail++; System.out.println("  FAIL " + what); }
    }

    /** Applies {@code configured} as a build of the given kind would, and reports the scale that resulted. */
    static double scaleAfter(String configured, boolean developmentBuild, String environment) {
        SessionLifetime.setScale(1.0);
        SessionTokenKeysInitializer.applyLifetimeScale(configured, developmentBuild, environment);
        return SessionLifetime.getScale();
    }

    public static void main(String[] args) {
        System.out.println("a build for a real deployment ignores it, however it is set:");
        check("production", scaleAfter("0.5", false, "production") == 1.0);
        check("staging", scaleAfter("0.5", false, "staging") == 1.0);
        check("even at the smallest factor", scaleAfter("0.005", false, "production") == 1.0);

        System.out.println("a development build honours it:");
        check("a factor is applied", scaleAfter("0.5", true, "development") == 0.5);
        check("surrounding whitespace is tolerated", scaleAfter(" 0.5 ", true, "development") == 0.5);

        System.out.println("and still refuses anything that is not a shortening:");
        check("a factor above 1", scaleAfter("2", true, "development") == 1.0);
        check("zero", scaleAfter("0", true, "development") == 1.0);
        check("not a number", scaleAfter("soon", true, "development") == 1.0);

        System.out.println("and treats not being set as not being set:");
        check("absent", scaleAfter(null, true, "development") == 1.0);
        check("blank", scaleAfter("  ", true, "development") == 1.0);
        check("an unresolved placeholder", scaleAfter("${{ SESSION_TOKEN_LIFETIME_SCALE }}", true, "development") == 1.0);

        SessionLifetime.setScale(1.0);
        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
