package dev.webfx.stack.session.state;

import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.ast.json.JsonObject;
import dev.webfx.platform.ast.json.ReadOnlyJsonArray;
import dev.webfx.platform.ast.json.ReadOnlyJsonObject;
import dev.webfx.stack.com.serial.SerialCodecManager;

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
        JsonObject rawJson = Json.parseObjectSilently(encodedState);
        if (rawJson == null)
            return encodedState;
        JsonObject json = Json.createObject();
        ReadOnlyJsonArray keys = rawJson.keys();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getString(i);
            json.set(key, (Object) SerialCodecManager.decodeFromJson(rawJson.get(key)));
        }
        return json;
    }

    public static String encodeState(Object state) {
        if (state == null)
            return null;
        if (!(state instanceof ReadOnlyJsonObject))
            return state.toString();
        ReadOnlyJsonObject json = (ReadOnlyJsonObject) state;
        JsonObject rawJson = Json.createObject();
        ReadOnlyJsonArray keys = json.keys();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.getString(i);
            rawJson.set(key, SerialCodecManager.encodeToJson(json.get(key)));
        }
        return rawJson.toJsonString();
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

    public static Object getUserId(Object state) {
        return getStateAttribute(state, USER_ID_ATTRIBUE_NAME);
    }

    public static Object setUserId(Object state, Object userId) {
        return setUserId(state, userId, true);
    }

    public static Object setUserId(Object state, Object userId, boolean override) {
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
