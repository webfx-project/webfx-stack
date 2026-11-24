package dev.webfx.stack.session.token;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The cases that decide whether sliding renewal is protection or decoration.
 *
 * <p>Written to break it rather than to watch it work. Three of these are the ones that matter, and each
 * corresponds to a way this design fails in production rather than in a test:
 *
 * <ul>
 *   <li><b>A session outlives its token</b> — if it does not, the 5 September incident happens again for
 *       every user, every access window, the moment identity tokens are required.</li>
 *   <li><b>A retired token ends the family</b> — this is the entire compromise signal. If it does not
 *       fire, everything else here is bookkeeping.</li>
 *   <li><b>A store that cannot answer changes nothing</b> — if a failed read ends sessions instead, a
 *       database blip becomes a mass logout on the one table that must never cause one.</li>
 * </ul>
 *
 * <p>No test framework, for the reason recorded in webfx-stack-authz-core: this repository declares no
 * JUnit. Run from main(); it exits non-zero while the issue stands.
 */
public class RenewalCheck {

    static int pass = 0, fail = 0;
    static final long NOW = 1_700_000_000_000L;

    static void check(String w, boolean ok) {
        if (ok) { pass++; System.out.println("  ok   " + w); } else { fail++; System.out.println("  FAIL " + w); }
    }

    /** A store in a map: enough to exercise every verdict, and it cannot be wrong about its own state. */
    static class FakeStore implements SessionFamilyStore {
        record Family(int generation, long lastRenewed, long absoluteExpiry, boolean revoked) {}
        final Map<String, Family> families = new HashMap<>();
        boolean unavailable;
        int opened;

        @Override
        public Future<String> open(Object principal, SessionTier tier, long absoluteExpiryMillis) {
            if (unavailable)
                return Future.failedFuture("store is down");
            String id = "fam-" + (++opened);
            families.put(id, new Family(0, NOW, absoluteExpiryMillis, false));
            return Future.succeededFuture(id);
        }

        @Override
        public Future<FamilyRenewal> renew(String familyId, int presentedGeneration, long nowMillis) {
            if (unavailable)
                return Future.failedFuture("store is down");
            Family family = families.get(familyId);
            if (family == null || family.revoked || nowMillis >= family.absoluteExpiry)
                return Future.succeededFuture(FamilyRenewal.ended());
            if (presentedGeneration == family.generation) {
                families.put(familyId, new Family(family.generation + 1, nowMillis, family.absoluteExpiry, false));
                return Future.succeededFuture(new FamilyRenewal(Verdict.RENEWED, family.generation + 1, family.absoluteExpiry));
            }
            if (presentedGeneration > family.generation)
                return Future.succeededFuture(FamilyRenewal.ended());
            // Mirrors ModalityAuthSessionStore.classify: one behind is the only holder anybody has heard
            // from, more than one means somebody else has presented a later generation
            if (presentedGeneration == family.generation - 1)
                return Future.succeededFuture(new FamilyRenewal(Verdict.CURRENT, family.generation, family.absoluteExpiry));
            return Future.succeededFuture(FamilyRenewal.reuseDetected());
        }

        @Override
        public Future<Void> revoke(String familyId, String reason) {
            Family family = families.get(familyId);
            if (family != null) {
                families.put(familyId, new Family(family.generation, family.lastRenewed, family.absoluteExpiry, true));
                revokedAt.put(familyId, NOW);
            }
            return Future.succeededFuture();
        }

        final Map<String, Long> revokedAt = new HashMap<>();

        // Nothing here signs other devices out; RevokeOtherSessionsCheck covers that.
        @Override
        public Future<List<String>> revokeOtherFamilies(Object principal, String exceptFamilyId, String reason) {
            return Future.succeededFuture(List.of());
        }

        // Nothing here polls; RevocationPollCheck is where reading revocations back is exercised.
        @Override
        public Future<RevocationPage> revokedSince(long sinceMillis, RevocationCursor after) {
            return Future.succeededFuture(RevocationPage.EMPTY);
        }
    }

    static SessionTokenService.TokenRenewal renew(IdentityToken presented, long nowMillis) {
        return SessionTokenService.renew(presented, SessionTier.FRONT_OFFICE, nowMillis).result();
    }

    public static void main(String[] args) {
        SerialCodecManager.registerSerialCodec(new CheckPrincipalSerialCodec());
        SignedToken.setKeys(List.of("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        CheckPrincipal user = new CheckPrincipal(42, 7);
        FakeStore store = new FakeStore();
        SessionFamilyStoreRegistry.register(store);

        System.out.println("a fresh login opens a family and mints its first token:");
        String token = SessionTokenService.mintForLogin(user, false).result();
        IdentityToken first = PrincipalToken.verify(token, NOW);
        check("a family was opened", first.familyId() != null);
        check("at generation zero", first.generation() == 0);
        check("usable now", first.isWithinAccessWindow(System.currentTimeMillis()));
        check("not due for renewal the moment it is minted", !first.isRenewalDue(System.currentTimeMillis()));

        System.out.println("the tier comes from the login, not from whatever the thread happens to hold:");
        // The regression this guards: reading isBackoffice() inside mintForLogin would answer "false" for
        // every login in the system, because every gateway calls it after a database round trip — and every
        // staff session would silently get the front office's 90-day idle window and year-long cap.
        IdentityToken staff = PrincipalToken.verify(SessionTokenService.mintForLogin(user, true).result(), NOW);
        check("a back-office login is tiered BACK_OFFICE", staff.tier() == SessionTier.BACK_OFFICE);
        check("and gets the brisk cap, not the member one",
              staff.absoluteExpiryMillis() - System.currentTimeMillis() < 2 * 24 * 3600_000L);
        check("a front-office login is tiered FRONT_OFFICE", first.tier() == SessionTier.FRONT_OFFICE);

        System.out.println("the audience is settled at login, and no renewal moves it:");
        // What M0 buys, stated as a test. The `backoffice` flag is re-asserted on every message, so the
        // question is whether a caller can renew their way from one audience into the other by claiming
        // differently. Note the helper: renew() here always hints FRONT_OFFICE, standing in for a
        // connection now claiming to be the front office.
        FakeStore unmoved = new FakeStore();
        SessionFamilyStoreRegistry.register(unmoved);
        IdentityToken boLogin = PrincipalToken.verify(SessionTokenService.mintForLogin(user, true).result(), NOW);
        check("a back-office login is minted FOR the back office",
              boLogin.audience() == SessionAudience.BACK_OFFICE);
        long boLater = System.currentTimeMillis() + SessionLifetime.accessWindowMillis() + 1;
        IdentityToken boRenewed = PrincipalToken.verify(renew(boLogin, boLater).token(), boLater);
        check("and its renewal is still a back-office session, whatever the connection now says",
              boRenewed.audience() == SessionAudience.BACK_OFFICE);

        System.out.println("the methods are settled at login too, and a renewal cannot add one:");
        // The consequence worth stating out loud: a session cannot GAIN a factor by staying open.
        // Enrolling TOTP at 11am leaves a session minted at 9am saying what it said, so a step-up meant
        // to take effect at once needs a fresh mint rather than a renewal. Honest, and a trap for any
        // policy that assumes otherwise.
        FakeStore stepUp = new FakeStore();
        SessionFamilyStoreRegistry.register(stepUp);
        IdentityToken twoFactor = PrincipalToken.verify(SessionTokenService.mintForLogin(
            user, true, SessionAudience.BACK_OFFICE,
            SessionMethod.of(SessionMethod.PASSWORD, SessionMethod.TOTP)).result(), NOW);
        check("both methods are minted into the session",
              twoFactor.methods().equals(SessionMethod.of(SessionMethod.PASSWORD, SessionMethod.TOTP)));
        long stepUpLater = System.currentTimeMillis() + SessionLifetime.accessWindowMillis() + 1;
        IdentityToken stillBoth = PrincipalToken.verify(renew(twoFactor, stepUpLater).token(), stepUpLater);
        check("and a renewal carries both forward, neither dropped nor added to",
              stillBoth.methods().equals(SessionMethod.of(SessionMethod.PASSWORD, SessionMethod.TOTP)));

        SessionFamilyStoreRegistry.register(store);
        System.out.println("the access window is short, and the SESSION is not:");
        long minted = System.currentTimeMillis();
        check("renewal falls due inside the last quarter of the window",
              first.isRenewalDue(minted + SessionLifetime.accessWindowMillis() - 60_000));
        check("the token stops being usable at the end of it",
              !first.isWithinAccessWindow(minted + SessionLifetime.accessWindowMillis() + 1));
        check("but the session — the signed deadline — outlives it by a long way",
              first.sessionEndMillis() > minted + SessionLifetime.accessWindowMillis() * 10);
        check("with the absolute bound sitting at or beyond the session's own deadline",
              first.absoluteExpiryMillis() >= first.sessionEndMillis());

        System.out.println("a lapsed token is exchanged, not refused — the whole point:");
        long later = minted + SessionLifetime.accessWindowMillis() + 1;
        SessionTokenService.TokenRenewal renewal = renew(first, later);
        check("renewed", renewal.outcome() == SessionTokenService.TokenRenewal.Outcome.RENEWED);
        IdentityToken second = PrincipalToken.verify(renewal.token(), later);
        check("same person", user.equals(second.principal()));
        check("same family", first.familyId().equals(second.familyId()));
        check("next generation", second.generation() == first.generation() + 1);
        check("usable again", second.isWithinAccessWindow(later));

        // Renew once more, so the family moves TWO generations past the token held below. One behind is
        // forgiven by design; two means somebody else has presented a later generation.
        renew(second, later + 1);
        System.out.println("the retired token, once another generation has been claimed:");
        long muchLater = later + 2;
        SessionTokenService.TokenRenewal reuse = renew(first, muchLater);
        check("the session is ENDED, not merely refused this once",
              reuse.outcome() == SessionTokenService.TokenRenewal.Outcome.ENDED);
        check("no replacement token is handed out", reuse.token() == null);
        check("the family is revoked, so the OTHER holder dies too",
              store.families.get(first.familyId()).revoked());
        // Whoever else holds a copy is refused on their NEXT MESSAGE rather than at their next renewal,
        // which is up to an access window away — and on this instance without waiting to poll for its
        // own write.
        check("and refused on sight from now on, without waiting for a renewal",
              RevokedFamilies.isRevoked(first.familyId()));
        check("even the legitimate current token no longer renews",
              renew(second, muchLater).outcome() == SessionTokenService.TokenRenewal.Outcome.ENDED);

        System.out.println("the second tab that lost the race is not a thief:");
        FakeStore tabs = new FakeStore();
        SessionFamilyStoreRegistry.register(tabs);
        IdentityToken shared = PrincipalToken.verify(SessionTokenService.mintForLogin(user, false).result(), NOW);
        SessionTokenService.TokenRenewal winner = renew(shared, later);
        SessionTokenService.TokenRenewal loser = renew(shared, later + 1); // same token, moments apart
        check("the winner is renewed", winner.outcome() == SessionTokenService.TokenRenewal.Outcome.RENEWED);
        check("the loser is renewed too, rather than killed",
              loser.outcome() == SessionTokenService.TokenRenewal.Outcome.RENEWED);
        check("and both end up on the same generation",
              PrincipalToken.verify(loser.token(), later).generation()
              == PrincipalToken.verify(winner.token(), later).generation());
        check("the family is alive", !tabs.families.get(shared.familyId()).revoked());

        System.out.println("the absolute bound is the one thing renewal cannot move:");
        FakeStore capped = new FakeStore();
        SessionFamilyStoreRegistry.register(capped);
        IdentityToken bounded = PrincipalToken.verify(SessionTokenService.mintForLogin(user, false).result(), NOW);
        long pastTheCap = bounded.absoluteExpiryMillis() + 1;
        check("past its cap, the session ends",
              renew(bounded, pastTheCap).outcome() == SessionTokenService.TokenRenewal.Outcome.ENDED);

        System.out.println("a store that cannot answer is NOT a verdict:");
        FakeStore down = new FakeStore();
        SessionFamilyStoreRegistry.register(down);
        IdentityToken live = PrincipalToken.verify(SessionTokenService.mintForLogin(user, false).result(), NOW);
        down.unavailable = true;
        SessionTokenService.TokenRenewal blip = renew(live, later);
        check("the session is kept, not ended",
              blip.outcome() == SessionTokenService.TokenRenewal.Outcome.KEEP);
        check("and no token is invented for it", blip.token() == null);
        down.unavailable = false;
        check("it renews normally once the store answers again",
              renew(live, later).outcome() == SessionTokenService.TokenRenewal.Outcome.RENEWED);

        System.out.println("a token minted before families existed is upgraded, not refused:");
        FakeStore upgrade = new FakeStore();
        SessionFamilyStoreRegistry.register(upgrade);
        String legacy = SignedToken.mint(
            "{\"$codec\":\"CheckPrincipal\",\"personId\":42,\"accountId\":7}", NOW + 12 * 3600_000L);
        IdentityToken legacyIdentity = PrincipalToken.verify(legacy, NOW);
        SessionTokenService.TokenRenewal upgraded = renew(legacyIdentity, NOW);
        check("renewed rather than ended",
              upgraded.outcome() == SessionTokenService.TokenRenewal.Outcome.RENEWED);
        IdentityToken now = PrincipalToken.verify(upgraded.token(), NOW);
        check("the same person is carried forward, not a new one", user.equals(now.principal()));
        check("it now has a family to rotate in", now.familyId() != null);
        check("and a tier", now.tier() == SessionTier.FRONT_OFFICE);
        check("and the audience that grants least, since it proved none",
              now.audience() == SessionAudience.FRONT_OFFICE);

        // The one case where the audience and the tier must part company, and the escalation M0 closes.
        // The hint is the client's own `backoffice` claim. The tier may take it — every value it could
        // adopt only SHORTENS the session — but an audience taken from it would let anyone holding an
        // old front-office token renew it into a back-office one, having passed no back-office condition
        // on the way: no second factor, no account check. If this ever starts passing bo, that is the
        // hole reopening.
        SessionFamilyStoreRegistry.register(new FakeStore());
        IdentityToken hinted = PrincipalToken.verify(
            SessionTokenService.renew(legacyIdentity, SessionTier.BACK_OFFICE, NOW).result().token(), NOW);
        check("a back-office HINT still moves the tier, which is safe", hinted.tier() == SessionTier.BACK_OFFICE);
        check("but NEVER the audience — that would be an escalation by claim",
              hinted.audience() == SessionAudience.FRONT_OFFICE);
        check("and no methods are invented for it either",
              hinted.methods().isEmpty());
        SessionFamilyStoreRegistry.register(upgrade);

        System.out.println("logging out ends the family, not just the caller's copy of the token:");
        FakeStore out = new FakeStore();
        SessionFamilyStoreRegistry.register(out);
        IdentityToken loggingOut = PrincipalToken.verify(SessionTokenService.mintForLogin(user, false).result(), NOW);
        // The family is read from the state the syncer wrote after verifying the signature — never from
        // anything a caller sent. A caller who could name a family could end a stranger's session by
        // guessing an id, which would make logout a denial-of-service primitive.
        ThreadLocalStateHolder.runWithState(
            StateAccessor.setSessionFamilyId(StateAccessor.createEmptyState(), loggingOut.familyId()),
            () -> SessionTokenService.revokeCurrentSessionFamily().result());
        check("the family is revoked", out.families.get(loggingOut.familyId()).revoked());
        check("and refused on sight, so the other tabs stop at their next message",
              RevokedFamilies.isRevoked(loggingOut.familyId()));
        check("so the token stops renewing, even though its signature still holds",
              renew(loggingOut, NOW + 1000).outcome() == SessionTokenService.TokenRenewal.Outcome.ENDED);

        System.out.println("...and a logout with no family to end is not an error:");
        // Most sessions on the day this ships: a legacy token, or a client presenting none at all.
        check("no family in the state is a no-op, not a failure",
              ThreadLocalStateHolder.runWithState(StateAccessor.createEmptyState(),
                  () -> SessionTokenService.revokeCurrentSessionFamily().succeeded()));

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
