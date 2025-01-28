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

    private final static String SERVER_SESSION_ID_ATTRIBUE_NAME = "sessionId";
    private final static String USER_ID_ATTRIBUE_NAME = "userId";
    private final static String RUN_ID_ATTRIBUE_NAME = "runId";
    private final static String BACKOFFICE_ATTRIBUTE_NAME = "backoffice";

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
                Console.log("⛔️ Couldn't decode session state '" + key + "':", e);
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
        return (String) getStateAttribute(state, SERVER_SESSION_ID_ATTRIBUE_NAME);
    }

    public static Object setServerSessionId(Object state, String serverSessionId) {
        return setServerSessionId(state, serverSessionId, true);
    }

    public static Object setServerSessionId(Object state, String serverSessionId, boolean override) {
        return setStateAttribute(state, SERVER_SESSION_ID_ATTRIBUE_NAME, serverSessionId, override);
    }

    public static Object createServerSessionIdState(String serverSessionId) {
        return setServerSessionId(null, serverSessionId);
    }

    public static Object getUserId(Object state) {
        return getStateAttribute(state, USER_ID_ATTRIBUE_NAME);
    }

    public static Object setUserId(Object state, Object userId) {
        return setUserId(state, userId, true);
    }

    public static Object setUserId(Object state, Object userId, boolean override) {
        return setStateAttribute(state, USER_ID_ATTRIBUE_NAME, userId, override);
    }

    public static Object createUserIdState(Object userId) {
        return setUserId(null, userId);
    }

    public static String getRunId(Object state) {
        return (String) getStateAttribute(state, RUN_ID_ATTRIBUE_NAME);
    }

    public static Object setRunId(Object state, String runId) {
        return setRunId(state, runId, true);
    }

    public static Object setRunId(Object state, String runId, boolean override) {
        return setStateAttribute(state, RUN_ID_ATTRIBUE_NAME, runId, override);
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
