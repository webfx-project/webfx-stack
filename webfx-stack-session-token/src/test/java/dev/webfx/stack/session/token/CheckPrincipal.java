package dev.webfx.stack.session.token;

import java.util.Objects;

/**
 * The identity the checks in this module carry, standing in for whatever an application's own principal
 * type happens to be.
 *
 * <p><b>Why a stand-in rather than a real one.</b> These checks used to mint Modality's
 * {@code ModalityUserPrincipal}, which put a dependency from this module onto an application module ABOVE
 * it. The stack has to build without the application it serves, and a test-scoped dependency is still a
 * dependency — it appears in the pom, it must resolve, and it makes a lower layer unbuildable on its own.
 *
 * <p>Nothing is lost by standing in, because the token layer knows only two things about a principal:
 * that a registered codec can encode it, and that it can be compared. The second is load-bearing, which
 * is why this has VALUE equality: a principal whose equality is by object identity passes a round trip
 * and still breaks the authorization cache and the login-transition check in the real system, silently.
 * That failure was found once, by these checks, when ids came back as a different numeric type.
 *
 * <p>That Modality's own principals survive the round trip is a Modality question, and it is asked where
 * that dependency is legal: {@code ModalityPrincipalTokenCheck}, in modality-crm-server-authsession-plugin.
 */
public final class CheckPrincipal {

    // Object, not Integer, and that is the lesson rather than an accident: a codec hands back whatever
    // numeric type the payload happened to parse to — a Byte for a small id — so a principal that insists
    // on Integer throws on the way back in. The real principals take Object for exactly this reason.
    private final Object personId;
    private final Object accountId;

    public CheckPrincipal(Object personId, Object accountId) {
        this.personId = personId;
        this.accountId = accountId;
    }

    public Object getPersonId() {
        return personId;
    }

    public Object getAccountId() {
        return accountId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof CheckPrincipal that))
            return false;
        // Compared as numbers, not as boxed types, for the same reason the fields are Object.
        return numbersEqual(personId, that.personId) && numbersEqual(accountId, that.accountId);
    }

    private static boolean numbersEqual(Object a, Object b) {
        if (a == null || b == null)
            return a == b;
        return a instanceof Number x && b instanceof Number y ? x.longValue() == y.longValue() : a.equals(b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asLong(personId), asLong(accountId));
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    @Override
    public String toString() {
        return "CheckPrincipal(" + personId + ", " + accountId + ")";
    }
}
