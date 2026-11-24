package dev.webfx.stack.session.token;

import dev.webfx.platform.console.Console;

import java.util.Collection;

/**
 * Where the application says how to TELL a revoked family's clients that they are over, if it can tell
 * them at all.
 *
 * <h3>What this adds to revocation, which already works</h3>
 *
 * <p>Nothing about enforcement. A revoked family is refused at its next message by {@link RevokedFamilies}
 * and at its next renewal by the store, and both of those hold whether or not anything registers here.
 * What is missing without it is the client's own knowledge: a browser that has already fetched a screenful
 * of personal data is refused the NEXT thing it asks for, but it is not asked for anything until somebody
 * clicks. Until then the data sits on screen, readable by whoever is holding the machine — which in the
 * case this whole control exists for is not its owner.
 *
 * <p><b>One exception to "nothing about enforcement", and it is not this package's to fix.</b> While
 * {@link IdentityTokenPolicy#isTokenRequired()} is off — the shipped default — a token that is authentic
 * but past its access window is not treated as a logout at all: the syncer leaves that session alone and
 * returns before the revoked-family check is reached, so such a session is refused neither on sight nor at
 * renewal, and this announcement is the only thing that ends it. That is an argument for turning the flip
 * on, not for relying on a message a client may ignore.
 *
 * <p>So this is the difference between "signed out at the next click" and "signed out now". It is a
 * courtesy to a cooperating client, never a guarantee: a client that ignores the message, or is offline,
 * or is not a browser at all, keeps whatever it already has until its access window runs out. Do not grow
 * an enforcement decision on top of it.
 *
 * <h3>Why a registry rather than a call</h3>
 *
 * <p>This package knows which families are over. It has no idea whether anything is connected, and must
 * not: pushing needs the server's push layer, which sits above it, and a deployment may have no push
 * clients at all. Same shape as {@link SessionFamilyStoreRegistry}, for the same reason.
 *
 * @author Claude Code
 */
public final class RevocationBroadcastRegistry {

    /**
     * Told which families have just become known-revoked here — whether this instance performed the
     * revocation or learned of it from the store a moment ago.
     *
     * <p>Called with newly-known families only, never with the whole set. The poll re-reads a safety
     * window on a timer and would otherwise re-announce the same revocations every few minutes, which
     * for a cooperating client is a redundant message and for the log is noise that hides the real one.
     */
    @FunctionalInterface
    public interface RevocationBroadcaster {
        void onFamiliesRevoked(Collection<String> familyIds);
    }

    private static volatile RevocationBroadcaster broadcaster;

    private RevocationBroadcastRegistry() {}

    /**
     * Installs the broadcaster. Last registration wins; registering twice is harmless, and null takes the
     * announcement away again (which only a test has reason to do).
     *
     * <p>Announced at boot rather than left to be inferred, because the difference it makes — a revoked
     * session's screen going at once instead of at the next click — is invisible in normal running and
     * would be noticed only during an incident, which is the worst moment to discover it was never wired.
     */
    public static void register(RevocationBroadcaster broadcaster) {
        RevocationBroadcastRegistry.broadcaster = broadcaster;
        if (broadcaster != null)
            Console.log("🛡 Revoked sessions are announced to their connected clients — a revocation now"
                        + " reaches the screen instead of waiting for the next click");
    }

    /**
     * Announces the families, if anything is listening.
     *
     * <p>Catches {@link Throwable}, and this is not defensive habit. The callers are the local revocation
     * path — where a throw would fail a logout that has ALREADY happened in the database, telling the user
     * their session is still live when it is not — and the revocation poll, the one loop that must keep
     * running for every other instance's revocations to be seen here at all. Announcing is the least
     * important thing either of them does, so it is the thing that gives way.
     */
    static void broadcast(Collection<String> familyIds) {
        RevocationBroadcaster b = broadcaster;
        if (b == null || familyIds == null || familyIds.isEmpty())
            return;
        try {
            b.onFamiliesRevoked(familyIds);
        } catch (Throwable e) {
            Console.log("⚠️ Could not announce " + familyIds.size() + " revoked session family(ies) to their"
                        + " clients — they will be refused at their next message anyway: " + e);
        }
    }
}
