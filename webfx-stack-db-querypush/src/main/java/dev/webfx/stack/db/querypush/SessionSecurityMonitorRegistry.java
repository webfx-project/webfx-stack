package dev.webfx.stack.db.querypush;

import dev.webfx.platform.console.Console;

import java.util.function.Supplier;

/**
 * Where the session machinery publishes its figures for the monitor to collect.
 *
 * <p>An inversion, for the same reason the session family store is one: the monitor owns the place a
 * number appears and knows nothing about what produces it, while the module that knows about tokens,
 * families and revocation decides what is worth reporting. The alternative — the query-push server
 * reading session state directly — would make a reporting module depend on the security machinery it
 * reports on, which is a dependency pointing the wrong way and a compile-time one at that.
 *
 * <p>Nothing registered means no security section in the snapshot, which is exactly what a deployment
 * without the session machinery should show.
 *
 * <p><b>A known trade.</b> Avoiding a reporting→security dependency creates a security→reporting one:
 * the plugin that publishes these figures compiles against this module, and so against the query stack
 * behind it, which is more than five numbers deserve. The tidy answer is a small neutral module holding
 * this registry and its DTO, owned by neither side. Worth doing when a second publisher appears; before
 * that it is a module per supplier.
 *
 * @author Claude Code
 */
public final class SessionSecurityMonitorRegistry {

    // Written on the boot thread, read on whichever event loop serves the monitor call: volatile, or
    // the JMM permits a reader to go on seeing null and the section to never appear on that server.
    private static volatile Supplier<SessionSecurityMonitorInfo> supplier;

    /** One line, not one per poll: this is read every few seconds by every open monitor page. */
    private static volatile boolean reportedFailure;

    private SessionSecurityMonitorRegistry() {}

    /** Called once at boot by whoever holds the figures. Last registration wins. */
    public static void register(Supplier<SessionSecurityMonitorInfo> supplier) {
        SessionSecurityMonitorRegistry.supplier = supplier;
    }

    /**
     * The current figures, or null when nothing publishes them.
     *
     * <p>Never lets a reporting failure become a monitoring outage: this is called while building a
     * snapshot the page polls every few seconds, and a supplier that throws would take the whole
     * snapshot — clients, queries, memory, everything — down with it.
     */
    public static SessionSecurityMonitorInfo snapshot() {
        Supplier<SessionSecurityMonitorInfo> current = supplier;
        if (current == null)
            return null;
        try {
            return current.get();
        } catch (Throwable e) {
            // Throwable rather than RuntimeException: the supplier reaches across a module boundary, so
            // a deployment holding only half of it throws a LinkageError, which is an Error. Either way
            // the whole snapshot — clients, queries, memory — must not fail because one section cannot
            // be read.
            if (!reportedFailure) {
                reportedFailure = true;
                Console.log("⚠️ The session-security figures could not be read, so /monitor will not show"
                            + " them (reported once): " + e);
            }
            return null;
        }
    }
}
