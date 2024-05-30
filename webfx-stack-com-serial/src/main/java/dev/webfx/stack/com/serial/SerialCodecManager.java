package dev.webfx.stack.com.serial;

import dev.webfx.platform.ast.*;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.reflect.RArray;
import dev.webfx.platform.util.Numbers;
import dev.webfx.platform.util.Strings;
import dev.webfx.stack.com.serial.spi.SerialCodec;
import dev.webfx.stack.com.serial.spi.impl.ExceptionSerialCodec;
import dev.webfx.stack.com.serial.spi.impl.time.InstantSerialCodec;
import dev.webfx.stack.com.serial.spi.impl.time.LocalDateSerialCodec;
import dev.webfx.stack.com.serial.spi.impl.time.LocalDateTimeSerialCodec;
import dev.webfx.stack.com.serial.spi.impl.time.LocalTimeSerialCodec;

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

    public  final static String CODEC_ID_KEY = "$codec";
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
        if (object instanceof Instant)
            return encodePrefixedInstant((Instant) object);
        if (object instanceof LocalDate)
            return encodePrefixedLocalDate((LocalDate) object);
        if (object instanceof LocalDateTime)
            return encodePrefixedLocalDateTime((LocalDateTime) object);
        if (object instanceof LocalTime)
            return encodePrefixedLocalTime((LocalTime) object);
        // Other java objects are serialized into json
        return encodeToAstObject(object);
    }

    public static String encodePrefixedInstant(Instant instant) {
        return instant == null ? null : INSTANT_VALUE_PREFIX + instant;
    }

    public static String encodePrefixedLocalDate(LocalDate localDate) {
        return localDate == null ? null : LOCAL_DATE_VALUE_PREFIX + localDate;
    }

    public static String encodePrefixedLocalDateTime(LocalDateTime localDateTime) {
        return localDateTime == null ? null : LOCAL_DATE_TIME_VALUE_PREFIX + localDateTime;
    }

    public static String encodePrefixedLocalTime(LocalTime localTime) {
        return localTime == null ? null : LOCAL_TIME_VALUE_PREFIX + localTime;
    }

    public static String encodeInstant(Instant instant) {
        return Strings.toString(instant);
    }

    public static String encodeLocalDate(LocalDate localDate) {
        return Strings.toString(localDate);
    }

    public static String encodeLocalDateTime(LocalDateTime localDateTime) {
        return Strings.toString(localDateTime);
    }

    public static String encodeLocalTime(LocalTime localTime) {
        return Strings.toString(localTime);
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
        encoder.encode(javaObject, json);
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
        return decoder.decode(json);
    }

    public static <T> T decodeFromJson(Object object) {
        // Case 1: it's a json object => we call decodeFromAstObject(). The returned object may be any java object.
        if (object instanceof ReadOnlyAstObject)
            return decodeFromAstObject((ReadOnlyAstObject) object);
        // Case 2: it's a json array => we call decodePrimitiveArrayFromAstArray(). The returned object is always an Object[] array.
        if (object instanceof ReadOnlyAstArray)
            return (T) decodeAstArrayToJavaArray((ReadOnlyAstArray) object, Object.class);
        // Case 3: it's a String with instant value prefix => we decode and return the instant value
        if (object instanceof String) {
            String s = (String) object;
            if (s.startsWith(INSTANT_VALUE_PREFIX)) {
                try {
                    object = decodePrefixedInstant(s);
                } catch (Exception e) {
                    Console.log("Couldn't parse Instant, keeping the string");
                }
            } else if (s.startsWith(LOCAL_DATE_VALUE_PREFIX)) {
                try {
                    object = decodePrefixedLocalDate(s);
                } catch (Exception e) {
                    Console.log("Couldn't parse LocalDate, keeping the string");
                }
            } else if (s.startsWith(LOCAL_DATE_TIME_VALUE_PREFIX)) {
                try {
                    object = decodePrefixedLocalDateTime(s);
                } catch (Exception e) {
                    Console.log("Couldn't parse LocalDateTime, keeping the string");
                }
            } else if (s.startsWith(LOCAL_TIME_VALUE_PREFIX)) {
                try {
                    object = decodePrefixedLocalTime(s);
                } catch (Exception e) {
                    Console.log("Couldn't parse LocalTime, keeping the string");
                }
            }
        }
        // Case 4: it's something else => we assume it's a value that don't need decoding and return it as is
        return (T) object;
    }

    public static Instant decodePrefixedInstant(String encodedInstant) {
        return encodedInstant == null ? null : decodeInstant(encodedInstant.substring(INSTANT_VALUE_PREFIX.length()));
    }

    public static Instant decodeInstant(String encodedInstant) {
        return encodedInstant == null ? null : Instant.parse(encodedInstant);
    }

    public static LocalDate decodePrefixedLocalDate(String encodedLocalDate) {
        return encodedLocalDate == null ? null : decodeLocalDate(encodedLocalDate.substring(LOCAL_DATE_VALUE_PREFIX.length()));
    }

    public static LocalDate decodeLocalDate(String encodedLocalDate) {
        return encodedLocalDate == null ? null : LocalDate.parse(encodedLocalDate);
    }

    public static LocalDateTime decodePrefixedLocalDateTime(String encodedLocalDateTime) {
        return decodeLocalDateTime(encodedLocalDateTime.substring(LOCAL_DATE_TIME_VALUE_PREFIX.length()));
    }

    public static LocalDateTime decodeLocalDateTime(String encodedLocalDateTime) {
        return encodedLocalDateTime == null ? null : LocalDateTime.parse(encodedLocalDateTime);
    }

    public static LocalTime decodePrefixedLocalTime(String encodedLocalTime) {
        return encodedLocalTime == null ? null : decodeLocalTime(encodedLocalTime.substring(LOCAL_TIME_VALUE_PREFIX.length()));
    }

    public static LocalTime decodeLocalTime(String encodedLocalTime) {
        return encodedLocalTime == null ? null : LocalTime.parse(encodedLocalTime);
    }

    public static <T> ReadOnlyAstArray encodeJavaArrayToAstArray(T[] javaArray) {
        if (javaArray == null)
            return null;
        AstArray ca = AST.createArray();
        for (Object value : javaArray)
            ca.push(encodeToJson(value));
        return ca;
    }

    public static <T> T[] decodeAstArrayToJavaArray(ReadOnlyAstArray jsonArray, Class<T> expectedClass) {
        if (jsonArray == null)
            return null;
        int n = jsonArray.size();
        if (expectedClass == null) {
            if (n == 0)
                expectedClass = (Class<T>) Object.class;
            else
                expectedClass = (Class<T>) getJavaClass(jsonArray.getObject(0).getString(SerialCodecManager.CODEC_ID_KEY));
        }
        T[] array = (T[]) RArray.newInstance(expectedClass, n);
        for (int i = 0; i < n; i++)
            array[i] = decodeFromJson(jsonArray.getElement(i));
        return array;
    }

}
