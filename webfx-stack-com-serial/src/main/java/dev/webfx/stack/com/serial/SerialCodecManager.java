package dev.webfx.stack.com.serial;

import dev.webfx.platform.ast.json.*;
import dev.webfx.platform.util.Dates;
import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.com.serial.spi.SerialCodec;
import dev.webfx.stack.com.serial.spi.impl.ExceptionSerialCodec;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/*
 * @author Bruno Salmon
 */

public final class SerialCodecManager {

    public final static String CODEC_ID_KEY = "$codec";
    private final static String INSTANT_VALUE_PREFIX = "$instant:";

    private static final Map<Class<?>, SerialCodec<?>> encoders = new HashMap<>();
    private static final Map<String, SerialCodec<?>> decoders = new HashMap<>();
    private static final Map<String, Class<?>> javaClasses = new HashMap<>();

    static {
        registerSerialCodec(new ExceptionSerialCodec());
    }

    public static void registerSerialCodec(SerialCodec<?> codec) {
        Class<?> javaClass = codec.getJavaClass();
        encoders.put(javaClass, codec);
        decoders.put(codec.getCodecId(), codec);
        javaClasses.put(codec.getCodecId(), javaClass);
    }

    /* Not supported in J2ME CLDC
    public static void useSameJsonCodecAs(Class javaClass1, Class javaClass2) {
        registerJsonCodec(javaClass1, getJsonEncoder(javaClass2));
    } */

    public static <T> SerialCodec<T> getSerialEncoder(Class<T> javaClass) {
        for (Class<?> c = javaClass; c != null; c = c.getSuperclass()) {
            SerialCodec<T> codec = (SerialCodec<T>) encoders.get(c);
            if (codec != null)
                return codec;
        }
        return null;
    }

    public static <T> SerialCodec<T> getSerialDecoder(String codecId) {
        return (SerialCodec<T>) decoders.get(codecId);
    }

    public static Class<?> getJavaClass(String codecId) {
        return javaClasses.get(codecId);
    }

    public static Object encodeToJson(Object object) {
        // Keeping null and primitives as is
        if (object == null || object instanceof String || Numbers.isNumber(object) || object instanceof Boolean)
            return object;
        // Managing date objects (Instant, LocalDate and LocalDateTime)
        Instant instant = Dates.asInstant(object);
        if (instant == null) {
            LocalDateTime localDateTime = Dates.asLocalDateTime(object);
            if (localDateTime == null)
                localDateTime = Dates.toLocalDateTime(Dates.asLocalDate(object));
            if (localDateTime != null)
                instant = localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (instant != null)
            return INSTANT_VALUE_PREFIX + Dates.formatIso(instant);
        // Other java objects are serialized into json
        return encodeToJsonObject(object);
    }

    public static ReadOnlyJsonObject encodeToJsonObject(Object object) {
        if (object == null)
            return null;
        if (object instanceof ReadOnlyJsonObject)
            return (ReadOnlyJsonObject) object;
        return encodeJavaObjectToJsonObject(object, Json.createObject());
    }

    private static <T> JsonObject encodeJavaObjectToJsonObject(T javaObject, JsonObject json) {
        SerialCodec<T> encoder = getSerialEncoder((Class<T>) javaObject.getClass());
        if (encoder == null)
            throw new IllegalArgumentException("No SerialCodec for type: " + javaObject.getClass());
        json.set(CODEC_ID_KEY, encoder.getCodecId());
        encoder.encodeToJson(javaObject, json);
        return json;
    }

    public static <T> T decodeFromJsonObject(ReadOnlyJsonObject json) {
        if (json == null)
            return null;
        String codecId = json.getString(CODEC_ID_KEY);
        if (codecId == null) { // Particular case where no codec is specified
            // In that case we don't map the json object to a java object, but return another json object with decoded values
            JsonObject decodedJson = Json.createObject();
            ReadOnlyJsonArray keys = json.keys();
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.getElement(i);
                decodedJson.set(key, (Object) decodeFromJson(json.get(key)));
            }
            return (T) decodedJson;
        }
        SerialCodec<T> decoder = getSerialDecoder(codecId);
        if (decoder == null)
            throw new IllegalArgumentException("No SerialCodec found for id: '" + codecId + "' when trying to decode " + json.toJsonString());
        return decoder.decodeFromJson(json);
    }

    public static <T> T decodeFromJson(Object object) {
        // Case 1: it's a json object => we call decodeFromJsonObject(). The returned object may be any java object.
        if (object instanceof ReadOnlyJsonObject)
            return decodeFromJsonObject((ReadOnlyJsonObject) object);
        // Case 2: it's a json array => we call decodePrimitiveArrayFromJsonArray(). The return object is always an Object[] array.
        if (object instanceof ReadOnlyJsonArray)
            return (T) decodePrimitiveArrayFromJsonArray((ReadOnlyJsonArray) object);
        // Case 3: it's a String with instant value prefix => we decode and return the instant value
        if (object instanceof String) {
            String s = (String) object;
            if (s.startsWith(INSTANT_VALUE_PREFIX)) {
                s = s.substring(INSTANT_VALUE_PREFIX.length());
                Object instant = Dates.fastToInstantIfIsoString(s);
                if (instant != s)
                    object = instant;
            }
        }
        // Case 4: it's something else => we assume it's a value that don't need decoding and return it as is
        return (T) object;
    }

    public static ReadOnlyJsonArray encodePrimitiveArrayToJsonArray(Object[] primArray) {
        if (primArray == null)
            return null;
        JsonArray ca = Json.createArray();
        for (Object value : primArray)
            ca.push(encodeToJson(value));
        return ca;
    }

    public static Object[] decodePrimitiveArrayFromJsonArray(ReadOnlyJsonArray jsonArray) {
        if (jsonArray == null)
            return null;
        int n = jsonArray.size();
        Object[] array = new Object[n];
        for (int i = 0; i < n; i++)
            array[i] = decodeFromJson(jsonArray.getElement(i));
        return array;
    }

    public static ReadOnlyJsonArray encodeToJsonArray(Object[] array) {
        if (array == null)
            return null;
        JsonArray ca = Json.createArray();
        for (Object object : array)
            ca.push(encodeToJsonObject(object));
        return ca;
    }

    public static <T> T[] decodeFromJsonArray(ReadOnlyJsonArray ca, Class<T> expectedClass) {
        if (ca == null)
            return null;
        int n = ca.size();
        T[] array = (T[]) Array.newInstance(expectedClass, n);
        for (int i = 0; i < n; i++)
            array[i] = decodeFromJson(ca.getObject(i));
        return array;
    }
}
