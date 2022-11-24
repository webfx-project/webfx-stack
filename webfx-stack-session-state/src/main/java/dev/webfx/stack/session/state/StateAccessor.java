package dev.webfx.stack.session.state;

import dev.webfx.platform.json.Json;
import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.json.ReadOnlyJsonObject;

/**
 * @author Bruno Salmon
 */
public final class StateAccessor {

    private final static String SERVER_SESSION_ID_ATTRIBUE_NAME = "sessionId";
    private final static String USER_ID_ATTRIBUE_NAME = "userId";
    private final static String RUN_ID_ATTRIBUE_NAME = "runId";

    public static Object createEmptyState() {
        return Json.createObject();
    }

    public static Object decodeState(String encodedState) {
        return Json.parseObjectSilently(encodedState);
    }

    public static String encodeState(Object state) {
        return state == null ? null : state instanceof ReadOnlyJsonObject ? ((ReadOnlyJsonObject) state).toJsonString() : state.toString();
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

    public static String getUserId(Object state) {
        return (String) getStateAttribute(state, USER_ID_ATTRIBUE_NAME);
    }

    public static Object setUserId(Object state, String userId) {
        return setUserId(state, userId, true);
    }

    public static Object setUserId(Object state, String userId, boolean override) {
        return setStateAttribute(state, USER_ID_ATTRIBUE_NAME, userId, override);
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

    private static Object getStateAttribute(Object state, String name) {
        if (state instanceof ReadOnlyJsonObject)
            return ((ReadOnlyJsonObject) state).get(name);
        return null;
    }

    private static Object setStateAttribute(Object state, String name, Object value, boolean override) {
        if (value != null && state == null)
            state = createEmptyState();
        if (state instanceof JsonObject) {
            JsonObject jsonObject = (JsonObject) state;
            if (value != null && (!jsonObject.has(name) || override) || value == null && override && jsonObject.has(name)) {
                //System.out.println("state." + name + " = " + value);
                jsonObject.set(name, value);
            }
        }
        return state;
    }
}
