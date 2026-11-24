package dev.webfx.stack.db.query.cli;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.session.token.PrincipalToken;
import dev.webfx.stack.session.token.SessionAudience;
import dev.webfx.stack.session.token.SessionMethod;
import dev.webfx.stack.session.token.SessionTier;
import dev.webfx.stack.session.token.SignedToken;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static dev.webfx.stack.db.query.cli.Checks.check;
import static dev.webfx.stack.db.query.cli.Checks.report;
import static dev.webfx.stack.db.query.cli.Checks.section;

/**
 * That the CLI reads back the audience the server actually signed, and refuses when it cannot.
 *
 * <h3>Why this is checked against the real minter</h3>
 *
 * <p>{@link TokenAudience} knows two things the minting side keeps private: the token's layout, and the
 * {@code $aud} key. Re-stating them in a client is deliberate — a client holds a token as an opaque string and
 * has no business linking against the code that signs it — but a re-statement drifts, and the way it drifts is
 * the worst available: a renamed key would make the reader answer "cannot tell" for every token, the CLI would
 * refuse every run, and the obvious fix would be to relax the refusal.
 *
 * <p>So this check mints with {@code PrincipalToken.mint} and reads with {@link TokenAudience}, both ways
 * round. If either side moves, this fails rather than the tool going quiet.
 *
 * <h3>And why "cannot tell" must stay a refusal</h3>
 *
 * <p>The audience selects which read-scope arm a probe exercises. A run that could not confirm it, and
 * continued, would measure one arm and report the other — the failure this whole instrument was commissioned
 * after. Each malformed case below therefore asserts a refusal, not a default.
 *
 * <p><b>Probed by deletion:</b> removing the audience comparison from {@code TokenAudience.refusal}, and
 * separately the null check, each made this check fail; both were restored.
 *
 * @author Claude Code
 */
public class TokenAudienceCheck {

    static final long NOW = 1_700_000_000_000L;
    static final long ABSOLUTE_EXPIRY = NOW + 24 * 60 * 60 * 1000L;

    /** Stands in for an application's principal: the token layer only needs it to encode and compare. */
    record TestPrincipal(Object personId) {}

    static final class TestPrincipalSerialCodec extends SerialCodecBase<TestPrincipal> {
        TestPrincipalSerialCodec() {
            super(TestPrincipal.class, "ReadScopeCliTestPrincipal");
        }

        @Override
        public void encode(TestPrincipal arg, AstObject serial) {
            encodeObject(serial, "personId", arg.personId());
        }

        @Override
        public TestPrincipal decode(ReadOnlyAstObject serial) {
            return new TestPrincipal(decodeObject(serial, "personId"));
        }
    }

    static String mint(SessionAudience audience) {
        return PrincipalToken.mint(new TestPrincipal(6), "fam-1", 0, SessionTier.BACK_OFFICE,
                                   audience, SessionMethod.NONE, ABSOLUTE_EXPIRY, NOW);
    }

    public static void main(String[] args) {
        SerialCodecManager.registerSerialCodec(new TestPrincipalSerialCodec());
        SignedToken.setKeys(List.of("a-test-signing-key-which-is-not-a-real-one".getBytes(StandardCharsets.UTF_8)));

        section("The audience the real minter signs is the audience the CLI reads");
        check("a back-office token reads as back office",
              Audience.BACK_OFFICE == TokenAudience.of(mint(SessionAudience.BACK_OFFICE)));
        check("a front-office token reads as front office",
              Audience.FRONT_OFFICE == TokenAudience.of(mint(SessionAudience.FRONT_OFFICE)));
        check("the two are told apart, rather than both reading as one",
              TokenAudience.of(mint(SessionAudience.BACK_OFFICE)) != TokenAudience.of(mint(SessionAudience.FRONT_OFFICE)));

        section("The vocabulary has not drifted from the minting side");
        for (SessionAudience audience : SessionAudience.values())
            check("the server's code '" + audience.code() + "' is one this CLI knows",
                  Audience.ofTokenCode(audience.code()) != null);
        for (Audience audience : Audience.values())
            check("the CLI's code '" + audience.tokenCode() + "' is one the server mints",
                  SessionAudience.fromCode(audience.tokenCode()) != null);

        section("Everything that is not a readable audience is a refusal, never a default");
        check("no token at all cannot be read", TokenAudience.of(null) == null);
        check("an empty string cannot be read", TokenAudience.of("") == null);
        check("a string with no separator cannot be read", TokenAudience.of("notatoken") == null);
        check("a payload that is not base64url cannot be read", TokenAudience.of("!!!!.123.abc") == null);
        check("a payload that is not JSON cannot be read",
              TokenAudience.of(base64("hello").concat(".123.abc")) == null);
        check("a payload with no audience claim cannot be read",
              TokenAudience.of(base64("{\"personId\":6}").concat(".123.abc")) == null);
        check("an audience code this build does not know cannot be read",
              TokenAudience.of(base64("{\"$aud\":\"admin\"}").concat(".123.abc")) == null);
        check("a token minted with NO audience is not guessed from its tier",
              TokenAudience.of(PrincipalToken.mint(new TestPrincipal(6), "fam-1", 0, SessionTier.BACK_OFFICE,
                                                   null, SessionMethod.NONE, ABSOLUTE_EXPIRY, NOW)) == null);

        section("The refusal a run sees");
        check("a confirmed audience does not refuse",
              TokenAudience.refusal(mint(SessionAudience.BACK_OFFICE), Audience.BACK_OFFICE) == null);
        String wrongArm = TokenAudience.refusal(mint(SessionAudience.FRONT_OFFICE), Audience.BACK_OFFICE);
        check("the WRONG arm refuses", wrongArm != null);
        check("and the refusal names the arm actually obtained, which is what the operator needs to see",
              wrongArm != null && wrongArm.contains("front-office") && wrongArm.contains("back-office"));
        check("an unreadable token refuses rather than assuming the one asked for",
              TokenAudience.refusal("notatoken", Audience.BACK_OFFICE) != null);
        check("and no token at all refuses",
              TokenAudience.refusal(null, Audience.FRONT_OFFICE) != null);

        section("The reader never verifies, and must not be mistaken for a check that does");
        // A token whose payload says 'bo' but whose MAC is nonsense still reads as 'bo' here - deliberately.
        // The server rejects it outright, so the CLI's reading is only ever used to CONFIRM or REFUSE, never
        // to grant. Pinning it makes the limit explicit rather than leaving it to the comment.
        check("a token with a broken signature still parses, because this is not a verification",
              Audience.BACK_OFFICE == TokenAudience.of(base64("{\"$aud\":\"bo\"}") + ".123.notamac"));

        System.exit(report("TokenAudienceCheck"));
    }

    static String base64(String payload) {
        return java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
