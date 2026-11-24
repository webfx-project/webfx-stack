package dev.webfx.stack.db.query.cli;

import java.io.Console;
import java.util.Arrays;

/**
 * The only place a password enters this tool: a terminal prompt, typed by the person at the keyboard.
 *
 * <h3>Why it refuses without a terminal</h3>
 *
 * <p>No console means no echo control, and reading a password off a pipe would mean it had been put in a
 * script, an environment variable or a shell history on the way in — which is the arrangement this tool is
 * supposed to replace, not one it should accept quietly. The refusal names the alternative: sign in once, in
 * a terminal, and let the cached session carry the unattended runs.
 *
 * <p>That split is also what lets an agent run probes without ever handling a credential. It signs nobody in;
 * it uses a session somebody else established.
 *
 * @author Claude Code
 */
public final class ConsolePrompt {

    private ConsolePrompt() {}

    /**
     * Reads a password, returned as a {@code char[]} the caller must clear.
     *
     * <p>A {@code char[]} rather than a {@code String} because a String stays in the heap until it is
     * collected, and a heap dump taken in between — from a crash, or from anyone with the same uid — has it.
     * The window cannot be closed entirely (the credentials record and the serial codec both hold a String on
     * the way to the wire), but it can be kept to the length of one call instead of the length of the process.
     */
    public static char[] readPassword(String prompt) {
        Console console = System.console();
        if (console == null)
            throw new IllegalStateException(
                "There is no terminal to type a password into. Run 'signin' from a terminal once; afterwards,"
                + " unattended runs use the cached session and need no password.");
        char[] password = console.readPassword("%s", prompt);
        if (password == null || password.length == 0) {
            if (password != null)
                Arrays.fill(password, '\0');
            throw new IllegalStateException("No password was entered.");
        }
        return password;
    }

    /** Reads a line of ordinary, echoed input — a username, never a secret. */
    public static String readLine(String prompt) {
        Console console = System.console();
        if (console == null)
            throw new IllegalStateException("There is no terminal to read '" + prompt + "' from."
                                            + " Pass it as an option instead.");
        String line = console.readLine("%s", prompt);
        return line == null ? null : line.strip();
    }
}
