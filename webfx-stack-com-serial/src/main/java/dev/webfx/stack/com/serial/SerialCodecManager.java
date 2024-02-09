package dev.webfx.stack.com.serial;

import dev.webfx.platform.ast.*;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.util.Numbers;
import dev.webfx.stack.com.serial.spi.SerialCodec;
import dev.webfx.stack.com.serial.spi.impl.ExceptionSerialCodec;
import dev.webfx.stack.com.serial.spi.impl.time.InstantSerialCodec;
import dev.webfx.stack.com.serial.spi.impl.time.LocalDateSerialCodec;
import dev.webfx.stack.com.serial.spi.impl.time.LocalDateTimeSerialCodec;
import dev.webfx.stack.com.serial.spi.impl.time.LocalTimeSerialCodec;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/*
 * @author Bruno Salmon
 */

public final class SerialCodecManager {

    public final static String CODEC_ID_KEY = "$codec";
    private final static boolean ENCODE_TIME_VALUES_WITH_PREFIXED_STRING = true;
    private final static boolean DECODE_TIME_VALUES_WITH_PREFIXED_STRING = true;
    private final static String INSTANT_VALUE_PREFIX = "$I:";
    private final static String LOCAL_DATE_VALUE_PREFIX = "$LD:";
    private final static String LOCAL_DATE_TIME_VALUE_PREFIX = "$LDT:";
    private final static String LOCAL_TIME_VALUE_PREFIX = "$LT:";

    private static final Map<Class<?>, SerialCodec<?>> encoders = new HashMap<>();
    private static final Map<String, SerialCodec<?>> decoders = new HashMap<>();
    private static final Map<String, Class<?>> javaClasses = new HashMap<>();

    static {
        registerSerialCodec(new ExceptionSerialCodec());
        registerSerialCodec(new InstantSerialCodec());
        registerSerialCodec(new LocalDateSerialCodec());
        registerSerialCodec(new LocalDateTimeSerialCodec());
        registerSerialCodec(new LocalTimeSerialCodec());
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
        if (ENCODE_TIME_VALUES_WITH_PREFIXED_STRING) {
            if (object instanceof Instant)
                return INSTANT_VALUE_PREFIX + object;
            if (object instanceof LocalDate)
                return LOCAL_DATE_VALUE_PREFIX + object;
            if (object instanceof LocalDateTime)
                return LOCAL_DATE_TIME_VALUE_PREFIX + object;
            if (object instanceof LocalTime)
                return LOCAL_TIME_VALUE_PREFIX + object;
        }
        // Other java objects are serialized into json
        return encodeToAstObject(object);
    }

    public static ReadOnlyAstObject encodeToAstObject(Object object) {
        if (object == null)
            return null;
        if (object instanceof ReadOnlyAstObject)
            return (ReadOnlyAstObject) object;
        return encodeJavaObjectToAstObject(object, AST.createObject());
    }

    private static <T> AstObject encodeJavaObjectToAstObject(T javaObject, AstObject json) {
        SerialCodec<T> encoder = getSerialEncoder((Class<T>) javaObject.getClass());
        if (encoder == null)
            throw new IllegalArgumentException("No SerialCodec for type: " + javaObject.getClass());
        json.set(CODEC_ID_KEY, encoder.getCodecId());
        encoder.encodeToJson(javaObject, json);
        return json;
    }

    public static <T> T decodeFromAstObject(ReadOnlyAstObject json) {
        if (json == null)
            return null;
        String codecId = json.getString(CODEC_ID_KEY);
        if (codecId == null) { // Particular case where no codec is specified
            // In that case we don't map the json object to a java object, but return another json object with decoded values
            AstObject decodedJson = AST.createObject();
            ReadOnlyAstArray keys = json.keys();
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.getElement(i);
                decodedJson.set(key, (Object) decodeFromJson(json.get(key)));
            }
            return (T) decodedJson;
        }
        SerialCodec<T> decoder = getSerialDecoder(codecId);
        if (decoder == null)
            throw new IllegalArgumentException("No SerialCodec found for id: '" + codecId + "' when trying to decode " + Json.formatNode(json));
        return decoder.decodeFromJson(json);
    }

    public static <T> T decodeFromJson(Object object) {
        // Case 1: it's a json object => we call decodeFromAstObject(). The returned object may be any java object.
        if (object instanceof ReadOnlyAstObject)
            return decodeFromAstObject((ReadOnlyAstObject) object);
        // Case 2: it's a json array => we call decodePrimitiveArrayFromAstArray(). The returned object is always an Object[] array.
        if (object instanceof ReadOnlyAstArray)
            return (T) decodePrimitiveArrayFromAstArray((ReadOnlyAstArray) object);
        // Case 3: it's a String with instant value prefix => we decode and return the instant value
        if (object instanceof String && DECODE_TIME_VALUES_WITH_PREFIXED_STRING) {
            String s = (String) object;
            if (s.startsWith(INSTANT_VALUE_PREFIX)) {
                try {
                    object = Instant.parse(s.substring(INSTANT_VALUE_PREFIX.length()));
                } catch (Exception e) {
                    Console.log("Couldn't parse Instant, keeping the string");
                }
            } else if (s.startsWith(LOCAL_DATE_VALUE_PREFIX)) {
                try {
                    object = LocalDate.parse(s.substring(LOCAL_DATE_VALUE_PREFIX.length()));
                } catch (Exception e) {
                    Console.log("Couldn't parse LocalDate, keeping the string");
                }
            } else if (s.startsWith(LOCAL_DATE_TIME_VALUE_PREFIX)) {
                try {
                    object = LocalDateTime.parse(s.substring(LOCAL_DATE_TIME_VALUE_PREFIX.length()));
                } catch (Exception e) {
                    Console.log("Couldn't parse LocalDateTime, keeping the string");
                }
            } else if (s.startsWith(LOCAL_TIME_VALUE_PREFIX)) {
                try {
                    object = LocalTime.parse(s.substring(LOCAL_TIME_VALUE_PREFIX.length()));
                } catch (Exception e) {
                    Console.log("Couldn't parse LocalTime, keeping the string");
                }
            }
        }
        // Case 4: it's something else => we assume it's a value that don't need decoding and return it as is
        return (T) object;
    }

    public static ReadOnlyAstArray encodePrimitiveArrayToAstArray(Object[] primArray) {
        if (primArray == null)
            return null;
        AstArray ca = AST.createArray();
        for (Object value : primArray)
            ca.push(encodeToJson(value));
        return ca;
    }

    public static Object[] decodePrimitiveArrayFromAstArray(ReadOnlyAstArray jsonArray) {
        if (jsonArray == null)
            return null;
        int n = jsonArray.size();
        Object[] array = new Object[n];
        for (int i = 0; i < n; i++)
            array[i] = decodeFromJson(jsonArray.getElement(i));
        return array;
    }

    public static ReadOnlyAstArray encodeToAstArray(Object[] array) {
        if (array == null)
            return null;
        AstArray ca = AST.createArray();
        for (Object object : array)
            ca.push(encodeToAstObject(object));
        return ca;
    }

    public static <T> T[] decodeFromAstArray(ReadOnlyAstArray ca, Class<T> expectedClass) {
        if (ca == null)
            return null;
        int n = ca.size();
        T[] array = (T[]) Array.newInstance(expectedClass, n);
        for (int i = 0; i < n; i++)
            array[i] = decodeFromJson(ca.getObject(i));
        return array;
    }
}
