package dev.webfx.stack.session.token;

import dev.webfx.platform.console.Console;

/**
 * Where the application tells this package how to keep a session family, if it can keep one at all.
 *
 * <p>Same shape as the other registries the stack uses to ask the application a question it cannot
 * answer for itself. This package knows how to sign and how to count generations; it has no idea where a
 * row could live, and deliberately does not: the store needs shared state, which for one deployment is
 * Postgres and for another might be nothing at all.
 *
 * <p><b>Unregistered is a supported state, not a failure.</b> With no store there are no families, so
 * renewal still slides the session forward on server-observed traffic and still honours the absolute
 * bound — both of those are carried in the signed payload — but there is no generation to compare, and
 * therefore no rotation and no reuse detection. That is a real reduction in what the mechanism detects
 * and it is stated at boot rather than inferred, because a deployment silently running without the
 * compromise signal it believes it has is worse than one that never had it.
 *
 * @author Claude Code
 */
public final class SessionFamilyStoreRegistry {

    private static SessionFamilyStore store;

    private SessionFamilyStoreRegistry() {}

    /**
     * Installs the store. Last registration wins, and registering twice is harmless — several plugins in
     * one deployment may reasonably describe the same table.
     */
    public static void register(SessionFamilyStore store) {
        SessionFamilyStoreRegistry.store = store;
        Console.log("🔑 Session families are recorded — renewal rotates the token and a retired one ends the family");
    }

    /** The store, or null when nothing registered one. */
    public static SessionFamilyStore getStore() {
        return store;
    }
}
