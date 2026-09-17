package dev.webfx.stack.session.token;

import dev.webfx.stack.session.state.StateAccessor;
import dev.webfx.stack.com.serial.SerialCodecManager;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The two states a server can be in while this migrates: with a signing key, and without one.
 *
 * <p>The keyless cases are the ones that matter right now. Minting sits on the login path and
 * SignedToken.mint() throws when unconfigured, so a strict implementation would mean that deploying this
 * to a server whose key had not been installed — which is currently every server — stopped everyone
 * logging in. These assert that it degrades to exactly the old behaviour instead: an identity delivered,
 * no token attached, nothing thrown.
 *
 * <p>They stop being the desired behaviour at the flip. When clients require a token, a server that
 * cannot mint cannot authenticate anyone, and it should refuse to start rather than hand out identities
 * it cannot prove. Change these then; do not let them quietly outlive the migration they exist for.
 */
public class MintCheck {
    static int pass=0, fail=0;
    static void check(String w, boolean ok){ if(ok){pass++;System.out.println("  ok   "+w);} else {fail++;System.out.println("  FAIL "+w);} }
    public static void main(String[] a) {
        SerialCodecManager.registerSerialCodec(new CheckPrincipalSerialCodec());
        CheckPrincipal user = new CheckPrincipal(42, 7);

        System.out.println("no key configured — every server today, and David's machine tomorrow:");
        SignedToken.setKeys(List.of());
        Object state = createFor(user);
        check("login still produces a state", state != null);
        check("the identity is still delivered", StateAccessor.getUserId(state) != null);
        check("no token, and no exception thrown", StateAccessor.getUserToken(state) == null);

        System.out.println("key configured, and no family store — the degraded but working shape:");
        SignedToken.setKeys(List.of("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        Object signed = createFor(user);
        String token = StateAccessor.getUserToken(signed);
        check("a token is attached", token != null);
        check("the identity is delivered alongside it", StateAccessor.getUserId(signed) != null);
        long now = System.currentTimeMillis();
        IdentityToken identity = PrincipalToken.verify(token, now);
        check("the token verifies back to the same principal", user.equals(identity.principal()));
        check("it carries no family, so nothing claims it can be rotated", identity.familyId() == null);
        check("it is still capped", identity.absoluteExpiryMillis() > now);
        check("it does not verify against another server's key",
              verifyUnderOtherKey(token) == null);

        System.out.println("\n"+pass+" passed, "+fail+" failed");
        if (fail>0) System.exit(1);
    }
    /** createFor is asynchronous now, but completes synchronously with no store to write to. */
    static Object createFor(Object principal) {
        return AuthenticatedState.createFor(principal, false).result();
    }
    static Object verifyUnderOtherKey(String token) {
        SignedToken.setKeys(List.of("ffffffffffffffffffffffffffffffff".getBytes(StandardCharsets.UTF_8)));
        IdentityToken identity = PrincipalToken.verify(token, System.currentTimeMillis());
        return identity == null ? null : identity.principal();
    }
}
