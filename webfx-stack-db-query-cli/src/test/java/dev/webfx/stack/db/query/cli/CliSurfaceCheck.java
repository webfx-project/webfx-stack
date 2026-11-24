package dev.webfx.stack.db.query.cli;

import dev.webfx.stack.db.query.QueryResult;

import static dev.webfx.stack.db.query.cli.Checks.check;
import static dev.webfx.stack.db.query.cli.Checks.report;
import static dev.webfx.stack.db.query.cli.Checks.section;

/**
 * The parts of the tool that decide what it asks and what it says — and each one's quiet failure.
 *
 * <p>Nothing here needs a server, and none of it is cosmetic. A mistyped option that is ignored runs the wrong
 * arm; a limit appended to a statement that already has one silently pages twice; a row count read as a count
 * turns "the caller can read everybody" into "the caller can read 100"; an endpoint whose port defaults wrong
 * connects in clear to a closed port, or to the wrong server; a log line carrying a literal puts an email
 * address in a file nobody is thinking about.
 *
 * <p><b>Probed by deletion</b> for the four guards that matter: the unknown-option refusal, the existing-paging
 * refusal, the count shape check, and the literal masking. Each removal made this check fail; all were
 * restored.
 *
 * @author Claude Code
 */
public class CliSurfaceCheck {

    public static void main(String[] args) {
        section("A command line says what it means, or it is an error");
        CliArgs.Spec spec = new CliArgs.Spec()
            .flag("rows").flag("help")
            .option("audience").option("limit").option("offset").option("server").option("c");
        CliArgs parsed = spec.parse(new String[] {"probe", "conversations-for-event",
                                                  "--audience", "back-office", "--limit=50", "--rows"});
        check("positionals are kept in order",
              "probe".equals(parsed.positional(0, null))
              && "conversations-for-event".equals(parsed.positional(1, null)));
        check("a spaced option value is read", "back-office".equals(parsed.value("audience", null)));
        check("an =-joined option value is read", 50 == parsed.intValue("limit", 100));
        check("a flag is read", parsed.flag("rows"));
        check("an absent flag is false", !parsed.flag("help"));
        check("an absent option falls back", 100 == parsed.intValue("offset", 100));

        check("an UNKNOWN option is refused, not ignored — a typo must not run the other arm",
              refuses(() -> spec.parse(new String[] {"--audiance", "back-office"})));
        check("and the refusal lists what is accepted here",
              messageOf(() -> spec.parse(new String[] {"--audiance", "x"})).contains("--audience"));
        check("an option with no value is refused",
              refuses(() -> spec.parse(new String[] {"--audience"})));
        check("a flag given a value is refused",
              refuses(() -> spec.parse(new String[] {"--rows=yes"})));
        check("a non-numeric number is refused by name",
              refuses(() -> spec.parse(new String[] {"--limit", "many"}).intValue("limit", 1)));
        check("a missing required option says why it is needed",
              messageOf(() -> spec.parse(new String[0]).required("server", "there is no default server"))
                  .contains("no default server"));
        check("-c carries a statement with spaces intact",
              "select count(1) as n from Person".equals(
                  spec.parse(new String[] {"-c", "select count(1) as n from Person"}).value("c", null)));

        section("Paging is the server's, and never doubled");
        check("a limit is appended",
              "select id from Conversation where event=$1 limit 100".equals(
                  DqlRunner.paged("select id from Conversation where event=$1", 100, 0)));
        check("an offset is appended only when it is not zero",
              "select id from Conversation limit 10 offset 20".equals(
                  DqlRunner.paged("select id from Conversation", 10, 20))
              && !DqlRunner.paged("select id from Conversation", 10, 0).contains("offset"));
        check("a trailing semicolon is removed, so the clause is not appended after it",
              "select id from Conversation limit 10".equals(
                  DqlRunner.paged("select id from Conversation;", 10, 0)));
        check("a statement that already ends with its own limit is REFUSED",
              refuses(() -> DqlRunner.paged("select id from Conversation limit 5", 100, 0)));
        check("so is one that already ends with an offset",
              refuses(() -> DqlRunner.paged("select id from Conversation limit 5 offset 5", 100, 0)));
        check("but a limit inside a subquery is left alone — it is not the statement's own clause",
              DqlRunner.paged("select id from ChatMessage where conversation in"
                              + " (select id from Conversation order by id limit 5) and agent", 100, 0)
                  .endsWith("limit 100"));
        check("while a top-level limit AFTER a subquery is still refused",
              refuses(() -> DqlRunner.paged("select id from ChatMessage where conversation in"
                                            + " (select id from Conversation) limit 5", 100, 0)));
        check("and a count statement's own parentheses are not mistaken for a subquery",
              DqlRunner.paged("select count(1) as n from Conversation where event=$1", 100, 0)
                  .endsWith("limit 100"));
        check("a non-positive limit is refused", refuses(() -> DqlRunner.paged("select id from X", 0, 0)));
        check("a negative offset is refused", refuses(() -> DqlRunner.paged("select id from X", 10, -1)));

        section("A count is a count, and a row count is not");
        check("a one-by-one numeric result reads as its number",
              249 == new DqlResult(new QueryResult(1, 1, new Object[] {249L}, new String[] {"n"})).count());
        check("a result with more than one row is NOT read as a count",
              refusesState(() -> new DqlResult(new QueryResult(2, 1, new Object[] {1L, 2L}, new String[] {"n"})).count()));
        // The shape the first real run actually returned: a Modality DQL entity query carries the entity's own
        // column as well as the select list, so `select count(1) as n from Conversation` came back as 1 row of
        // 2 columns. Reading position 0 would have read an id as a count - a number, silently wrong.
        // The shape the first real run returned: Modality DQL prepends a column of its own, and the result
        // carries NO column names, so the count is the last column and position 0 is the placeholder. A
        // reader taking position 0 would have answered 1 for every probe - plausible and silently wrong.
        check("the count is the LAST column, not position 0",
              249 == new DqlResult(new QueryResult(1, 2, new Object[] {1L, 249L}, null)).count());
        check("a single-column count still reads",
              249 == new DqlResult(new QueryResult(1, 1, new Object[] {249L}, null)).count());
        check("three columns are refused rather than guessed at",
              refusesState(() -> new DqlResult(new QueryResult(1, 3, new Object[] {1L, 2L, 3L}, null)).count()));
        check("and the refusal says Modality prepends a column, which is what explains the shape",
              messageOf(() -> new DqlResult(new QueryResult(1, 3, new Object[] {1L, 2L, 3L}, null)).count())
                  .contains("prepends"));
        check("a placeholder in the count's position is still refused when it is not a number",
              refusesState(() -> new DqlResult(new QueryResult(1, 2, new Object[] {1L, "249"}, null)).count()));
        check("a non-numeric single value is NOT read as a count",
              refusesState(() -> new DqlResult(new QueryResult(1, 1, new Object[] {"249"}, new String[] {"n"})).count()));
        check("rowCount is reported separately and is not the count",
              new DqlResult(new QueryResult(3, 1, new Object[] {1L, 2L, 3L}, new String[] {"id"})).rowCount() == 3);
        check("an empty result counts zero rows",
              new DqlResult(new QueryResult(0, 1, new Object[0], new String[] {"id"})).rowCount() == 0);

        section("An endpoint is the one that was asked for");
        check("wss defaults to 443, not 80",
              new ServerEndpoint("h", 443, true).equals(ServerEndpoint.parse("wss://h")));
        check("ws defaults to 80",
              new ServerEndpoint("h", 80, false).equals(ServerEndpoint.parse("ws://h")));
        check("https is accepted as the app's own origin, with the same default",
              new ServerEndpoint("h", 443, true).equals(ServerEndpoint.parse("https://h")));
        check("an explicit port wins",
              new ServerEndpoint("h", 8080, false).equals(ServerEndpoint.parse("http://h:8080")));
        check("a path after the host is ignored",
              new ServerEndpoint("h", 443, true).equals(ServerEndpoint.parse("wss://h/eventbus/websocket")));
        check("a scheme-less server is refused rather than guessed", ServerEndpoint.parse("h:443") == null);
        check("an unknown scheme is refused", ServerEndpoint.parse("ftp://h") == null);
        check("an empty host is refused", ServerEndpoint.parse("wss://") == null);
        check("a non-numeric port is refused", ServerEndpoint.parse("wss://h:https") == null);
        check("an out-of-range port is refused", ServerEndpoint.parse("wss://h:70000") == null);

        ServerEndpoint staging = new ServerEndpoint("backoffice.example.org", 443, true);
        check("a configuration that matches is accepted",
              staging.matchesConfiguration("backoffice.example.org", "443", Boolean.TRUE));
        check("the localhost:80 fallback does NOT match — this is what catches an unresolved variable",
              !staging.matchesConfiguration("localhost", "80", Boolean.FALSE));
        check("a right host on the wrong port does not match",
              !staging.matchesConfiguration("backoffice.example.org", "80", Boolean.TRUE));
        check("a right host and port without SSL does not match",
              !staging.matchesConfiguration("backoffice.example.org", "443", Boolean.FALSE));
        check("an unresolved (null) configuration does not match",
              !staging.matchesConfiguration(null, null, null));

        section("A run is logged by shape, never by value");
        String masked = RunLog.maskLiterals(
            "select count(1) as n from Person where email='someone@example.org' and id=59714");
        check("string literals are masked", !masked.contains("someone@example.org"));
        check("number literals are masked", !masked.contains("59714"));
        check("but the tables and columns are kept, which is what identifies the read",
              masked.contains("Person") && masked.contains("email"));
        check("double-quoted literals are masked too",
              !RunLog.maskLiterals("select id from Person where lastName=\"Dupont\"").contains("Dupont"));
        check("and backquoted ones",
              !RunLog.maskLiterals("select id from Person where lastName=`Dupont`").contains("Dupont"));
        check("a very long statement is truncated rather than filling the log",
              RunLog.maskLiterals("select " + "x,".repeat(400) + "y from Person").length() <= 201);
        check("no statement at all logs as none, rather than as null",
              "(none)".equals(RunLog.maskLiterals(null)));

        System.exit(report("CliSurfaceCheck"));
    }

    static boolean refuses(Runnable runnable) {
        try {
            runnable.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    static boolean refusesState(Runnable runnable) {
        try {
            runnable.run();
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }

    static String messageOf(Runnable runnable) {
        try {
            runnable.run();
            return "";
        } catch (RuntimeException e) {
            return e.getMessage() == null ? "" : e.getMessage();
        }
    }
}
