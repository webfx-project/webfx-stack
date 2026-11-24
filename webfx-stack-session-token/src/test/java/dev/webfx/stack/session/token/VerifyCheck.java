package dev.webfx.stack.session.token;

import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.session.state.LogoutUserId;
import dev.webfx.stack.session.state.StateAccessor;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The four cases ServerSideStateSessionSyncer.applyIdentityToken must distinguish.
 *
 * <p>It mirrors that method rather than calling it, because the syncer needs a live session and bus to be
 * driven directly. Keep the two in step: if the syncer's logic changes, change this. The case worth
 * guarding hardest is a token that is not OURS — forged, altered, or signed with a retired key — which must
 * never fall back to the identity the message claimed, or anyone could defeat the whole check by sending
 * rubbish and being handed the old, weaker path.
 *
 * <p>A token that IS ours and has merely expired is the one exception, and it is not a softening of that
 * rule. While the flip is off the claim stands, because a client sending no token at all is already
 * believed — refusing here bought nothing and cost every user a forced logout twelve hours after login.
 * Once the flip is on it is refused like anything else unproven, which the last block checks.
 *
 * <p>The no-token case is the migration path and asserts that behaviour is UNCHANGED for clients that do
 * not send one. That assertion stops being desirable at the flip, when a missing token must start being
 * refused; change it then rather than letting it quietly outlive its purpose.
 */
public class VerifyCheck {
    static int pass=0, fail=0;
    static void check(String w, boolean ok){ if(ok){pass++;System.out.println("  ok   "+w);} else {fail++;System.out.println("  FAIL "+w);} }

    // Mirrors ServerSideStateSessionSyncer.applyIdentityToken.
    static void applyIdentityToken(Object clientState) {
        // Cleared FIRST, as the syncer does, and mirrored here because this is the whole of its security:
        // the field arrives inside a message the caller wrote, and guards grant back-office access on it.
        StateAccessor.setVerifiedBackoffice(clientState, null);
        String token = StateAccessor.getUserToken(clientState);
        if (token == null || token.isEmpty()) {
            // Mirrored too, or the flip's PRIMARY case is silently untested: inverting the condition in the
            // syncer would leave every check below green while the thing the flip exists to do stopped
            // happening.
            boolean claimsRealIdentity = !LogoutUserId.isLogoutUserIdOrNull(StateAccessor.getUserId(clientState));
            if (IdentityTokenPolicy.refusesUntokenedClaim(claimsRealIdentity))
                StateAccessor.setUserId(clientState, LogoutUserId.LOGOUT_USER_ID);
            return;
        }
        long nowMillis = System.currentTimeMillis();
        IdentityToken identity = PrincipalToken.verify(token, nowMillis);
        if (identity != null) {
            StateAccessor.setVerifiedBackoffice(clientState, identity.audience() == SessionAudience.BACK_OFFICE);
            // Inside its access window the syncer honours the principal as it stands; past it, it waits for
            // a renewal and honours the same principal unless that renewal reports the session over. Both
            // land here, so this mirror stays about WHICH IDENTITY IS BELIEVED — what the renewal itself
            // decides is RenewalCheck's subject, and needs a store this mirror deliberately does not have.
            StateAccessor.setUserId(clientState, identity.principal());
        } else if (IdentityTokenPolicy.isTokenRequired() || !SignedToken.isAuthenticButExpired(token, nowMillis))
            StateAccessor.setUserId(clientState, LogoutUserId.LOGOUT_USER_ID);
        // else: ours, past its signed deadline, flip off — the claim stands and the state is left untouched
    }

    /** A token whose SESSION ends at the given moment, minted the way a login mints one. */
    static String mint(Object principal, long sessionEndMillis) {
        return PrincipalToken.mint(principal, null, 0, SessionTier.FRONT_OFFICE, SessionAudience.FRONT_OFFICE,
                                   SessionMethod.of(SessionMethod.PASSWORD), sessionEndMillis,
                                   System.currentTimeMillis());
    }

    public static void main(String[] a) {
        SerialCodecManager.registerSerialCodec(new CheckPrincipalSerialCodec());
        SignedToken.setKeys(List.of("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        CheckPrincipal real = new CheckPrincipal(42, 7);
        CheckPrincipal impostorClaim = new CheckPrincipal(1, 1);

        System.out.println("no token — the migration path, behaviour must be unchanged:");
        Object s1 = StateAccessor.createUserIdState(real);
        applyIdentityToken(s1);
        check("claimed identity is left exactly as it was", real.equals(StateAccessor.getUserId(s1)));

        System.out.println("valid token — the proven identity wins over the claim:");
        Object s2 = StateAccessor.createUserIdState(impostorClaim);
        StateAccessor.setUserToken(s2, mint(real, System.currentTimeMillis() + 60_000));
        applyIdentityToken(s2);
        check("token's principal replaces the claim", real.equals(StateAccessor.getUserId(s2)));
        check("the claimed identity is discarded", !impostorClaim.equals(StateAccessor.getUserId(s2)));

        System.out.println("token present but not valid — must NOT fall back to the claim:");
        Object s3 = StateAccessor.createUserIdState(impostorClaim);
        StateAccessor.setUserToken(s3, "not-a-real-token");
        applyIdentityToken(s3);
        check("forged token yields logged out", LogoutUserId.isLogoutUserIdOrNull(StateAccessor.getUserId(s3)));
        check("the claim is NOT honoured as a fallback", !impostorClaim.equals(StateAccessor.getUserId(s3)));

        System.out.println("token ours but expired — the claim stands while the flip is off:");
        Object s4 = StateAccessor.createUserIdState(real);
        StateAccessor.setUserToken(s4, mint(real, System.currentTimeMillis() - 1));
        applyIdentityToken(s4);
        check("expired token leaves the claimed identity in place", real.equals(StateAccessor.getUserId(s4)));
        check("expired token does NOT end the session", !LogoutUserId.isLogoutUserIdOrNull(StateAccessor.getUserId(s4)));

        System.out.println("...and is refused once a token is required:");
        IdentityTokenPolicy.setTokenRequired(true);
        try {
            Object s4on = StateAccessor.createUserIdState(real);
            StateAccessor.setUserToken(s4on, mint(real, System.currentTimeMillis() - 1));
            applyIdentityToken(s4on);
            check("expired token yields logged out", LogoutUserId.isLogoutUserIdOrNull(StateAccessor.getUserId(s4on)));
            Object s4forged = StateAccessor.createUserIdState(impostorClaim);
            StateAccessor.setUserToken(s4forged, "not-a-real-token");
            applyIdentityToken(s4forged);
            check("forged token still yields logged out", LogoutUserId.isLogoutUserIdOrNull(StateAccessor.getUserId(s4forged)));
            // The flip's whole purpose, and the case the earlier blocks never reach because they all carry
            // a token: a bare claim with nothing behind it.
            Object s4bare = StateAccessor.createUserIdState(real);
            applyIdentityToken(s4bare);
            check("a claim with no token at all is refused", LogoutUserId.isLogoutUserIdOrNull(StateAccessor.getUserId(s4bare)));
            Object s4anon = StateAccessor.createUserIdState(LogoutUserId.LOGOUT_USER_ID);
            applyIdentityToken(s4anon);
            check("a logged-out client is left alone, not 'refused'", LogoutUserId.isLogoutUserIdOrNull(StateAccessor.getUserId(s4anon)));
        } finally {
            IdentityTokenPolicy.setTokenRequired(false); // static and global: never leak it into the next block
        }

        System.out.println("after a key rotation that dropped the old key:");
        Object s5 = StateAccessor.createUserIdState(real);
        StateAccessor.setUserToken(s5, mint(real, System.currentTimeMillis() + 60_000));
        SignedToken.setKeys(List.of("ffffffffffffffffffffffffffffffff".getBytes(StandardCharsets.UTF_8)));
        applyIdentityToken(s5);
        check("token from the retired key yields logged out", LogoutUserId.isLogoutUserIdOrNull(StateAccessor.getUserId(s5)));

        System.out.println("the audience a GUARD reads comes only from a verified token:");
        // What M1 grants on. Every case here is a way a caller could try to hand themselves the answer.
        Object bo = StateAccessor.createUserIdState(real);
        StateAccessor.setUserToken(bo, PrincipalToken.mint(real, null, 0, SessionTier.BACK_OFFICE,
            SessionAudience.BACK_OFFICE, SessionMethod.of(SessionMethod.PASSWORD),
            System.currentTimeMillis() + 600_000, System.currentTimeMillis()));
        applyIdentityToken(bo);
        check("a back-office token yields a back-office session",
              Boolean.TRUE.equals(StateAccessor.getVerifiedBackoffice(bo)));

        Object fo = StateAccessor.createUserIdState(real);
        StateAccessor.setUserToken(fo, mint(real, System.currentTimeMillis() + 600_000));
        applyIdentityToken(fo);
        check("a front-office token yields FALSE, not null",
              Boolean.FALSE.equals(StateAccessor.getVerifiedBackoffice(fo)));

        // The attack, and the reason the field is cleared before anything is read: the caller writes this
        // message. Leaving it set would let anyone grant themselves every back-office endpoint by adding
        // one header field — no token, no login, nothing to verify.
        Object forged = StateAccessor.createUserIdState(real);
        StateAccessor.setVerifiedBackoffice(forged, true);
        applyIdentityToken(forged);
        check("a caller who writes it into their own message has it WIPED",
              StateAccessor.getVerifiedBackoffice(forged) == null);

        // Same, but with a real front-office token beside the lie: the token must win, and it must not
        // merely be absent — FALSE is what refuses, null is what refuses, TRUE is what grants.
        Object forgedWithToken = StateAccessor.createUserIdState(real);
        StateAccessor.setUserToken(forgedWithToken, mint(real, System.currentTimeMillis() + 600_000));
        StateAccessor.setVerifiedBackoffice(forgedWithToken, true);
        applyIdentityToken(forgedWithToken);
        check("and a front-office token overrides it rather than being ignored",
              Boolean.FALSE.equals(StateAccessor.getVerifiedBackoffice(forgedWithToken)));

        // A token that did not hold leaves no audience at all, which is what a guard must refuse on.
        Object forgedToken = StateAccessor.createUserIdState(real);
        StateAccessor.setUserToken(forgedToken, mint(real, System.currentTimeMillis() + 600_000) + "X");
        StateAccessor.setVerifiedBackoffice(forgedToken, true);
        applyIdentityToken(forgedToken);
        check("a token that fails verification leaves it null, lie and all",
              StateAccessor.getVerifiedBackoffice(forgedToken) == null);

        System.out.println("\n"+pass+" passed, "+fail+" failed");
        if (fail>0) System.exit(1);
    }
}
