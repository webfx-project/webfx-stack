package dev.webfx.stack.db.query.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * One line per statement run: who asked, what was asked, and how many rows came back. Never the rows.
 *
 * <h3>Why the result is not in here</h3>
 *
 * <p>This tool's whole subject is what a caller can read about people, so its log is the last place that
 * should hold any of it. A count is the useful part anyway — every invariant is a count — and it can be
 * written anywhere without anybody having to think about where the file ends up.
 *
 * <p>Literals in the statement are masked the same way {@code ClientQueryGuard} masks a refused query's shape,
 * and for the same reason: a statement's TEXT identifies what was asked, while a literal inside it is where an
 * email address or a name would be.
 *
 * <h3>Why there is a log at all</h3>
 *
 * <p>The comparison that justifies this tool is against direct database access, and one of the two things it
 * has that the database does not is a trace somebody will actually read. A probe run that left nothing behind
 * would give up the argument.
 *
 * @author Claude Code
 */
public final class RunLog {

    public static final String FILE_NAME = "read-scope-cli-runs.log";

    private final Path file;

    public RunLog(Path directory) {
        this.file = directory.resolve(FILE_NAME);
    }

    public Path file() {
        return file;
    }

    /**
     * Appends one line.
     *
     * @param caller    the signed-in caller, as the client session knows them
     * @param audience  the audience that was CONFIRMED, not the one requested
     * @param what      the probe's name, or "-c" / "-f" for a statement given on the command line
     * @param statement the DQL, whose literals are masked here
     * @param rowCount  how many rows came back, or -1 when the statement did not complete
     */
    public void append(String caller, Audience audience, String what, String statement, long rowCount) {
        String line = Instant.now() + "\t" + nullSafe(caller) + "\t"
                      + (audience == null ? "-" : audience.optionValue()) + "\t"
                      + nullSafe(what) + "\t" + maskLiterals(statement) + "\t"
                      + (rowCount < 0 ? "(incomplete)" : String.valueOf(rowCount)) + "\n";
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, line.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // A run that cannot be logged is still a run worth finishing, but it must not look logged.
            System.err.println("⚠️  Could not write to " + file + ": " + e.getMessage());
        }
    }

    /**
     * The statement with string and number literals replaced.
     *
     * <p>Kept in step with {@code ClientQueryGuard.maskLiterals} on purpose — the two produce the same shape
     * for the same statement, so a refused query in the server's log and the run that sent it can be matched
     * up by eye. Re-stated rather than called because this module has no reason to depend on the guard, and
     * {@code RunLogCheck} pins the behaviour either way.
     *
     * <p><b>The patterns are written unrolled AND possessive, which the guard's are not.</b> The obvious
     * {@code '(?:[^']|'')*'} — an alternation under a star — costs a stack frame per character in
     * {@code java.util.regex} and throws {@link StackOverflowError} on a quoted literal of about five
     * thousand. Unrolling it to {@code '[^']*(?:''[^']*)*'} moves the recursion from per-character to
     * per-escaped-quote, which still gives way at about twenty thousand {@code ''} pairs. Making both
     * quantifiers possessive removes it altogether: half a million plain characters and twenty thousand
     * escaped pairs both mask fine. Nothing is lost by it, because {@code [^']} cannot match the delimiter,
     * so there was never a backtrack to give up.
     *
     * <p>The output is identical to the guard's for every input the guard survives, so the two still agree on
     * any statement either can mask. A statement reaching THIS method comes from the operator, so an overflow
     * here would be a crash rather than an attack — but it would land while logging the run, which is the one
     * record the comparison against direct database access rests on. The guard's copy takes statements from
     * clients, where the same shape is reachable on purpose; that is reported separately and is not this
     * module's to change.
     */
    public static String maskLiterals(String statement) {
        if (statement == null)
            return "(none)";
        String masked = statement
            .replaceAll("'[^']*+(?:''[^']*+)*+'", "'?'")
            .replaceAll("\"[^\"]*+(?:\"\"[^\"]*+)*+\"", "\"?\"")
            .replaceAll("`[^`]*+(?:``[^`]*+)*+`", "`?`")
            .replaceAll("\\b\\d+(?:\\.\\d+)?\\b", "?")
            .replaceAll("\\s+", " ")
            .trim();
        return masked.length() > 200 ? masked.substring(0, 200) + "…" : masked;
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
