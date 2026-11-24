package dev.webfx.stack.db.query.cli;

import static dev.webfx.stack.db.query.cli.Checks.check;
import static dev.webfx.stack.db.query.cli.Checks.report;
import static dev.webfx.stack.db.query.cli.Checks.section;

/**
 * What the run log may and may not contain.
 *
 * <p>The masking is the part worth pinning: it is the only thing standing between a statement that selected
 * somebody by name and a log line naming them. The long-literal cases are here because the obvious way to
 * write these patterns — {@code '(?:[^']|'')*'} — costs a stack frame per character and throws
 * {@link StackOverflowError} at about five thousand, which would land while logging a run rather than while
 * running it.
 *
 * @author Claude Code
 */
public final class RunLogCheck {

    public static void main(String[] args) {
        section("A literal is never what the log records");
        check("a single-quoted literal is masked (and count(1)'s number with it)",
              RunLog.maskLiterals("select count(1) from Person where email='someone@example.org'")
                  .equals("select count(?) from Person where email='?'"));
        check("a double-quoted literal is masked, being as personal as a single-quoted one",
              RunLog.maskLiterals("select x from Person where name=\"Jean Dupont\"")
                  .equals("select x from Person where name=\"?\""));
        check("a backtick literal is masked",
              RunLog.maskLiterals("select x from Person where name=`Jean Dupont`")
                  .equals("select x from Person where name=`?`"));
        check("an escaped quote inside a literal does not end it",
              RunLog.maskLiterals("select x from Y where z='it''s'").equals("select x from Y where z='?'"));
        check("two literals are masked separately, not merged into one",
              RunLog.maskLiterals("select x from Y where a='p' and b='q'")
                  .equals("select x from Y where a='?' and b='?'"));
        check("an empty literal is still masked",
              RunLog.maskLiterals("select x from Y where z=''").equals("select x from Y where z='?'"));
        check("numbers are masked too, an id being personal enough",
              RunLog.maskLiterals("select x from Y where event=1857").equals("select x from Y where event=?"));

        section("The shape survives, because the shape is the useful part");
        check("entity and column names are kept",
              RunLog.maskLiterals("select count(1) from Conversation where event=1857")
                  .contains("Conversation"));
        check("whitespace is collapsed so one statement has one shape",
              RunLog.maskLiterals("select   x\n  from Y").equals("select x from Y"));
        check("a very long statement is truncated rather than logged whole",
              RunLog.maskLiterals("select " + "x,".repeat(400) + "y from Z").endsWith("…"));
        check("null is reported, not thrown on", RunLog.maskLiterals(null).equals("(none)"));

        section("Masking does not fall over on a long literal");
        // Written unrolled for exactly this: the alternation-under-a-star form throws here at ~5,000.
        for (int length : new int[] {5_000, 50_000, 500_000}) {
            String statement = "select x from Y where z='" + "a".repeat(length) + "'";
            String masked;
            try {
                masked = RunLog.maskLiterals(statement);
            } catch (StackOverflowError e) {
                masked = "(StackOverflowError)";
            }
            check("a literal of " + length + " characters masks to the same shape as a short one",
                  "select x from Y where z='?'".equals(masked));
        }
        String nested = "select x from Y where z='" + "it''s ".repeat(20_000) + "'";
        String maskedNested;
        try {
            maskedNested = RunLog.maskLiterals(nested);
        } catch (StackOverflowError e) {
            maskedNested = "(StackOverflowError)";
        }
        check("a long literal full of escaped quotes masks to the same shape",
              "select x from Y where z='?'".equals(maskedNested));

        section("The file is named, so a run can say where it went");
        check("the log file name is the one the runbook names",
              RunLog.FILE_NAME.equals("read-scope-cli-runs.log"));
        check("the log lives where it is told to",
              new RunLog(java.nio.file.Path.of("somewhere")).file()
                  .equals(java.nio.file.Path.of("somewhere", RunLog.FILE_NAME)));

        System.exit(report("RunLogCheck"));
    }
}
