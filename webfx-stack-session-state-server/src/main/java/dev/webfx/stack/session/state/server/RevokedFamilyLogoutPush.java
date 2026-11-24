package dev.webfx.stack.session.state.server;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.push.server.PushServerService;
import dev.webfx.stack.session.state.LogoutUserId;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.session.token.RevocationBroadcastRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Tells the clients of a revoked session family, at once, that their session is over.
 *
 * <h3>What this is for</h3>
 *
 * <p>Revocation already works without it. A revoked family is refused on its next message and at its next
 * renewal, so nothing NEW can be fetched from the moment it is revoked. But a browser is not asked for
 * anything until somebody clicks, and until then the personal data already on screen stays on screen —
 * readable by whoever is holding the machine, which in the case this control exists for is not its owner.
 * Somebody who ends a session from another device reasonably expects the screen to go, and without this it
 * does not.
 *
 * <p>There is one case where this is doing more than that, and it should be fixed at its source rather than
 * leaned on here: while the identity-token flip is off, a token that is authentic but past its access window
 * is left alone by the syncer, which returns before it consults the revoked families. Such a session is
 * refused neither on sight nor at renewal, so this push is the only thing that ends it — and a push, as
 * below, is exactly what may be ignored.
 *
 * <p>So: when a family becomes revoked, every connected client holding a token of that family is pushed a
 * logged-out state. The client applies it exactly as it applies the logged-out state it would have got at
 * its next message — the same mechanism a magic-link login already uses in the other direction — which
 * ends its session, clears its caches and puts the login form up.
 *
 * <h3>Where it is prompt, and where it is only quick</h3>
 *
 * <p>This instance announces its own revocations immediately. A revocation performed on ANOTHER instance
 * is announced here when the poll finds it, because the instances share a database and nothing else — so a
 * client connected elsewhere hears within one poll interval rather than instantly. That is still bounded by
 * a timer instead of by whether the user happens to click, which is the whole point.
 *
 * <h3>What it is not</h3>
 *
 * <p>A courtesy to a cooperating client, never enforcement. A client that ignores the push, or is offline,
 * or was never a browser, keeps whatever it already holds until its access window runs out — at most the
 * thirty minutes a token is good for. That bound comes from the token, not from here, and this cannot
 * tighten it: a token already issued cannot be recalled. Nothing may be built on the assumption that the
 * push arrived.
 *
 * @author Claude Code
 */
public final class RevokedFamilyLogoutPush {

    private RevokedFamilyLogoutPush() {}

    /**
     * Installs the announcement. Call once at boot, from wherever the session store is set up — the two
     * belong together, since without a store there are no families to revoke.
     */
    public static void install() {
        RevocationBroadcastRegistry.register(RevokedFamilyLogoutPush::pushLogoutToFamilies);
    }

    private static void pushLogoutToFamilies(Collection<String> familyIds) {
        // One walk of the connected clients for the whole batch. A bulk revocation is precisely when this
        // could otherwise become quadratic, and precisely when it must not.
        List<Object> runIds = PushServerService.snapshotClientRunIdsOfFamilies(familyIds);
        // The caller's own client, when a caller is what started this. An ordinary logout revokes the
        // caller's family and then pushes a logout through the gateway anyway (LogoutPush), so without this
        // every logout would send the same client the same message twice — and would log a line saying a
        // revoked session had been told, on the commonest action in the system. Dropping it leaves the log
        // meaning what it says: somebody OTHER than the person who asked has been signed out. Null on the
        // poll's thread, which has no caller, so a revocation learned from another instance tells everyone.
        Object callerRunId = ThreadLocalStateHolder.getRunId();
        List<Object> toTell = new ArrayList<>(runIds.size());
        for (Object runId : runIds)
            if (!Objects.equals(runId, callerRunId))
                toTell.add(runId);
        if (toTell.isEmpty())
            return; // nobody to tell: the usual case, since the client is normally on the other instance
        Console.log("🛡 Telling " + toTell.size() + " connected client(s) of " + familyIds.size()
                    + " revoked session family(ies) that they are over — they are refused from now on"
                    + " either way, this is so their screens do not wait for a click");
        for (Object runId : toTell) {
            // Each push is independent: one client that has gone away must not stop the others being told.
            // A failure here means only that this client will find out at its next message instead.
            PushServerService.pushState(StateAccessor.createUserIdState(LogoutUserId.LOGOUT_USER_ID), runId)
                .onFailure(e -> Console.log("⚠️ Could not tell a revoked session's client that it is"
                                            + " over — it will be refused at its next message: " + e));
        }
    }
}
