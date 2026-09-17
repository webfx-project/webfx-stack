package dev.webfx.stack.session.state;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.session.Session;

/**
 * @author Bruno Salmon
 */
public final class StateAccessor {

    private final static String SERVER_SESSION_ID_ATTRIBUTE_NAME = "sessionId";
    private final static String USER_ID_ATTRIBUTE_NAME = "userId";
    // The signed, expiring proof of who the caller is. Carried beside userId rather than replacing it
    // while clients are migrated; the point of the exercise is the day userId alone stops being believed.
    private final static String USER_TOKEN_ATTRIBUTE_NAME = "userToken";
    private final static String RUN_ID_ATTRIBUTE_NAME = "runId";
    private final static String BACKOFFICE_ATTRIBUTE_NAME = "backoffice";
    // Invariant per-connection client facts, sent once at (re)connection and kept in the server session.
    private final static String CLIENT_VERSION_ATTRIBUTE_NAME = "clientVersion";
    private final static String PWA_ATTRIBUTE_NAME = "pwa";
    // Compact "browser|os|deviceType" device profile, sent once at connection (for the /monitor breakdowns).
    private final static String CLIENT_PROFILE_ATTRIBUTE_NAME = "clientProfile";
    private final static String SERVER_RUN_ID_ATTRIBUTE_NAME = "serverRunId";
    // Stamped on every server-emitted state envelope; clients drop any envelope without it.
    private final static String SERVER_ORIGIN_ATTRIBUTE_NAME = "serverOrigin";
    private final static String CLIENT_ORIGIN_ATTRIBUTE_NAME = "clientOrigin"; // Stamped by the bridge on everything arriving from outside
    private final static String TOKEN_REQUIRED_ATTRIBUTE_NAME = "tokenRequired"; // Server->client: is a token required to claim an identity?
    // The session family named by the token this message presented, recorded once the signature has held.
    // SERVER-INTERNAL: written by the session syncer and read by whatever needs to act on the session as a
    // whole — logout, so far. It is never put on an outgoing state, which is built from scratch rather than
    // copied from this one; and it would not matter much if it were, since the client already carries the
    // same value inside its own token. Not a capability: reaching the store with it still needs a valid MAC.
    private final static String SESSION_FAMILY_ID_ATTRIBUTE_NAME = "sessionFamilyId";

    /** Unique ID generated once at server startup — changes on every restart. */
    private static final String SERVER_RUN_ID = "srv-" + System.currentTimeMillis();

    public static Object createEmptyState() {
        return AST.createObject();
    }

    public static Object createStateFromSession(Session session) {
        Object state = createEmptyState();
        setServerSessionId(state, SessionAccessor.getServerSessionId(session));
        setUserId(state, SessionAccessor.getUserId(session));
        setRunId(state, SessionAccessor.getRunId(session));
        setBackoffice(state, SessionAccessor.isBackoffice(session));
        return state;
    }

    public static Object decodeState(String encodedState) {
        ReadOnlyAstObject rawJson = Json.parseObjectSilently(encodedState);
        if (rawJson == null)
            return encodedState;
        AstObject json = AST.createObject();
        ReadOnlyAstArray keys = rawJson.keys();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getString(i);
            try {
                Object javaObject = SerialCodecManager.decodeFromJson(rawJson.get(key));
                json.set(key, javaObject);
            } catch (Exception e) {
                Console.error("Couldn't decode session state '" + key + "':", e);
            }
        }
        return json;
    }

    public static String encodeState(Object state) {
        if (state == null)
            return null;
        if (!(state instanceof ReadOnlyAstObject))
            return state.toString();
        ReadOnlyAstObject json = (ReadOnlyAstObject) state;
        AstObject rawJson = AST.createObject();
        ReadOnlyAstArray keys = json.keys();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getString(i);
            rawJson.set(key, SerialCodecManager.encodeToJson(json.get(key)));
        }
        return Json.formatNode(rawJson);
    }

    public static String getServerSessionId(Object state) {
        return (String) getStateAttribute(state, SERVER_SESSION_ID_ATTRIBUTE_NAME);
    }

    public static Object setServerSessionId(Object state, String serverSessionId) {
        return setServerSessionId(state, serverSessionId, true);
    }

    public static Object setServerSessionId(Object state, String serverSessionId, boolean override) {
        return setStateAttribute(state, SERVER_SESSION_ID_ATTRIBUTE_NAME, serverSessionId, override);
    }

    public static Object createServerSessionIdState(String serverSessionId) {
        return setServerSessionId(null, serverSessionId);
    }

    public static Object getUserId(Object state) {
        return getStateAttribute(state, USER_ID_ATTRIBUTE_NAME);
    }

    public static Object setUserId(Object state, Object userId) {
        return setUserId(state, userId, true);
    }

    public static Object setUserId(Object state, Object userId, boolean override) {
        return setStateAttribute(state, USER_ID_ATTRIBUTE_NAME, userId, override);
    }

    public static Object createUserIdState(Object userId) {
        return setUserId(null, userId);
    }

    /**
     * The caller's signed identity token, if it presented one.
     *
     * <p>Opaque here on purpose: this class is shared with clients, and minting or verifying needs a
     * secret that only a server may hold. A client's whole relationship with this value is to store what
     * it was given and send it back.
     */
    public static String getUserToken(Object state) {
        return (String) getStateAttribute(state, USER_TOKEN_ATTRIBUTE_NAME);
    }

    public static Object setUserToken(Object state, String userToken) {
        return setStateAttribute(state, USER_TOKEN_ATTRIBUTE_NAME, userToken, true);
    }

    /**
     * Whether this server requires a token to accept an identity — told to the client so it can stop
     * sending a claim the server would ignore.
     *
     * <p>Server to client only, and the client is free to disbelieve it: acting on it makes the client
     * send LESS, never more, so a wrong value costs a redundant field or a corrected identity, never an
     * unearned one. That asymmetry is why it can be communicated at all — the reverse direction, a client
     * telling the server what to require, would be the whole vulnerability restated.
     */
    public static Boolean isTokenRequired(Object state) {
        return (Boolean) getStateAttribute(state, TOKEN_REQUIRED_ATTRIBUTE_NAME);
    }

    public static Object setTokenRequired(Object state, Boolean tokenRequired, boolean override) {
        return setStateAttribute(state, TOKEN_REQUIRED_ATTRIBUTE_NAME, tokenRequired, override);
    }

    /**
     * The session family the caller's token belongs to, or null when it presented none this server could read.
     *
     * <p>Only ever set from a token whose signature has already been verified, so unlike the userId beside it
     * this is not something a caller can choose. That is what makes it safe to act on — revoking a family ends
     * every session sharing it, which would be a gift to an attacker if the family could be named at will.
     */
    public static String getSessionFamilyId(Object state) {
        return (String) getStateAttribute(state, SESSION_FAMILY_ID_ATTRIBUTE_NAME);
    }

    public static Object setSessionFamilyId(Object state, String sessionFamilyId) {
        return setStateAttribute(state, SESSION_FAMILY_ID_ATTRIBUTE_NAME, sessionFamilyId, true);
    }

    public static String getRunId(Object state) {
        return (String) getStateAttribute(state, RUN_ID_ATTRIBUTE_NAME);
    }

    public static Object setRunId(Object state, String runId) {
        return setRunId(state, runId, true);
    }

    public static Object setRunId(Object state, String runId, boolean override) {
        return setStateAttribute(state, RUN_ID_ATTRIBUTE_NAME, runId, override);
    }

    public static Boolean getBackoffice(Object state) {
        return (Boolean) getStateAttribute(state, BACKOFFICE_ATTRIBUTE_NAME);
    }

    public static Object setBackoffice(Object state, Boolean backoffice) {
        return setBackoffice(state, backoffice, true);
    }

    public static Object setBackoffice(Object state, Boolean backoffice, boolean override) {
        return setStateAttribute(state, BACKOFFICE_ATTRIBUTE_NAME, backoffice, override);
    }

    public static String getClientVersion(Object state) {
        return (String) getStateAttribute(state, CLIENT_VERSION_ATTRIBUTE_NAME);
    }

    public static Object setClientVersion(Object state, String clientVersion) {
        return setStateAttribute(state, CLIENT_VERSION_ATTRIBUTE_NAME, clientVersion, true);
    }

    public static Boolean getPwa(Object state) {
        return (Boolean) getStateAttribute(state, PWA_ATTRIBUTE_NAME);
    }

    public static Object setPwa(Object state, Boolean pwa) {
        return setStateAttribute(state, PWA_ATTRIBUTE_NAME, pwa, true);
    }

    public static String getClientProfile(Object state) {
        return (String) getStateAttribute(state, CLIENT_PROFILE_ATTRIBUTE_NAME);
    }

    public static Object setClientProfile(Object state, String clientProfile) {
        return setStateAttribute(state, CLIENT_PROFILE_ATTRIBUTE_NAME, clientProfile, true);
    }

    /** Get the server-generated run ID (unique per server process lifetime). */
    public static String getServerRunId() {
        return SERVER_RUN_ID;
    }

    public static String getServerRunId(Object state) {
        return (String) getStateAttribute(state, SERVER_RUN_ID_ATTRIBUTE_NAME);
    }

    public static Object setServerRunId(Object state, String serverRunId) {
        return setServerRunId(state, serverRunId, true);
    }

    public static Object setServerRunId(Object state, String serverRunId, boolean override) {
        return setStateAttribute(state, SERVER_RUN_ID_ATTRIBUTE_NAME, serverRunId, override);
    }

    public static Boolean getServerOrigin(Object state) {
        return (Boolean) getStateAttribute(state, SERVER_ORIGIN_ATTRIBUTE_NAME);
    }

    public static boolean isServerOrigin(Object state) {
        return Boolean.TRUE.equals(getServerOrigin(state));
    }

    public static Object setServerOrigin(Object state, Boolean serverOrigin) {
        return setServerOrigin(state, serverOrigin, true);
    }

    public static Object setServerOrigin(Object state, Boolean serverOrigin, boolean override) {
        return setStateAttribute(state, SERVER_ORIGIN_ATTRIBUTE_NAME, serverOrigin, override);
    }

    /**
     * Whether this call entered from outside — stamped by the SockJS bridge, never by a caller.
     *
     * <p>The mirror of {@link #isServerOrigin}, and trustworthy for the same reason read backwards. The
     * bridge is the one place a client message can enter, it sets this on every inbound frame, and it
     * OVERWRITES whatever arrived — so a caller sending {@code clientOrigin: false} is simply corrected.
     * Server-internal work never passes through the bridge and so never carries the stamp, which is what
     * lets a rule say "clients may not do this" while server code still can.
     *
     * <p>Absent means not-from-a-client, and that direction is deliberate: the stamp is applied by the
     * component that KNOWS, so its absence is evidence rather than ignorance. Note the scope, though —
     * this covers bus traffic. An HTTP endpoint is a different door and carries no stamp at all.
     */
    public static boolean isClientOrigin(Object state) {
        return Boolean.TRUE.equals(getStateAttribute(state, CLIENT_ORIGIN_ATTRIBUTE_NAME));
    }

    public static Object setClientOrigin(Object state, Boolean clientOrigin) {
        return setStateAttribute(state, CLIENT_ORIGIN_ATTRIBUTE_NAME, clientOrigin, true);
    }

    private static Object getStateAttribute(Object state, String name) {
        if (state instanceof ReadOnlyAstObject)
            return ((ReadOnlyAstObject) state).get(name);
        return null;
    }

    private static Object setStateAttribute(Object state, String name, Object value, boolean override) {
        if (value != null && state == null)
            state = createEmptyState();
        if (state instanceof AstObject) {
            AstObject jsonObject = (AstObject) state;
            if (value != null && (!jsonObject.has(name) || override) || value == null && override && jsonObject.has(name)) {
                //System.out.println("state." + name + " = " + value);
                jsonObject.set(name, value);
            }
        }
        return state;
    }
}
