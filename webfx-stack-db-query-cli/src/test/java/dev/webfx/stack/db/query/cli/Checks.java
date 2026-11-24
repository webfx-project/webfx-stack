package dev.webfx.stack.db.query.cli;

/**
 * The shared tally every {@code *Check} in this module uses.
 *
 * <p>No test framework, for the reason recorded in {@code webfx-stack-authz-core}: this repository declares no
 * JUnit. Each check runs from {@code main()} and exits non-zero while anything is wrong.
 *
 * @author Claude Code
 */
final class Checks {

    static int pass, fail;

    private Checks() {}

    static void check(String what, boolean ok) {
        if (ok) {
            pass++;
            System.out.println("  ok   " + what);
        } else {
            fail++;
            System.out.println("  FAIL " + what);
        }
    }

    static void section(String title) {
        System.out.println();
        System.out.println(title);
    }

    static int report(String name) {
        System.out.println();
        System.out.println(name + ": " + pass + " passed, " + fail + " failed");
        return fail == 0 ? 0 : 1;
    }
}
