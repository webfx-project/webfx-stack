package dev.webfx.stack.session.state;

import java.util.function.Supplier;

/**
 * Declares the SQL that opens a write transaction, when the application needs one.
 *
 * <p>Third sibling of {@link AuditActorRegistry} and {@link RestrictedPrincipalRegistry}, and the same
 * shape for the same reason: the stack has no idea what a transaction preamble would say, because that
 * is application SQL about application triggers. The application registers it once and the layer every
 * write passes through applies it, without either side knowing about the other.
 *
 * <p><b>Why the application does not simply send it.</b> It used to: the client put the statement in a
 * submit batch itself, which meant the client chose the privileges the transaction ran with. A
 * front-office caller could send the back-office preamble and have triggers treat its writes
 * accordingly. Splitting it in two fixes the half that matters — the caller still says *that* it needs
 * a preamble (a correctness question it knows the answer to, and gets loudly wrong if it guesses,
 * because the database raises when a trigger reads a setting nobody made), while the server alone says
 * *what* the preamble contains.
 *
 * <p><b>The resolver runs on the calling thread.</b> It is expected to read
 * {@link ThreadLocalStateHolder}, which is restored when the synchronous portion of a call returns —
 * so callers must resolve before their first async hop, never inside a queue that may run later and on
 * another thread. Same constraint, and the same reason, as
 * {@link AuditActorRegistry#currentActorId()}.
 *
 * <p>Unregistered is NOT a supported state for an application that asks for a preamble: a caller that
 * requests one and silently gets none would write under whatever the triggers default to, which is the
 * failure this class exists to prevent. {@link #currentPreambleStatement()} returns null so the caller
 * can refuse the write explicitly rather than proceed.
 *
 * @author Bruno Salmon
 */
public final class TransactionPreambleRegistry {

    private static Supplier<String> resolver;

    private TransactionPreambleRegistry() {}

    /**
     * Declares how to build the preamble for the transaction about to run. Last registration wins.
     * The supplier is called once per batch, on the calling thread, and may read the thread-local state
     * to decide what this particular caller is entitled to.
     */
    public static void registerResolver(Supplier<String> preambleResolver) {
        resolver = preambleResolver;
    }

    /** Whether an application has declared a preamble at all. */
    public static boolean hasResolver() {
        return resolver != null;
    }

    /**
     * The preamble for the current caller, or null when nothing is registered or the resolver declines.
     *
     * <p>Never throws: a resolver that fails returns null, and the caller refuses the write. That is the
     * conservative direction here — the same argument as
     * {@link RestrictedPrincipalRegistry#isCurrentUserRestricted()}. Failing to establish what a
     * transaction is allowed to do must not let it run as though it were allowed everything.
     */
    public static String currentPreambleStatement() {
        Supplier<String> r = resolver;
        if (r == null)
            return null;
        try {
            return r.get();
        } catch (Throwable e) {
            return null;
        }
    }
}
