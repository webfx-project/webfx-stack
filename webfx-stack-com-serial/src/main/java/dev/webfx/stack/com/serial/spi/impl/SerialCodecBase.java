package dev.webfx.stack.com.serial.spi.impl;

/*
 * @author Bruno Salmon
 */

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.SerialCodec;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public abstract class SerialCodecBase<T> implements SerialCodec<T> {

    public enum NullEncoding {
        NULL_VALUE_NOT_ALLOWED, // will raise an exception if the passed value is null
        NULL_VALUE_ALLOWED, // will accept a null value and encode it (ex: key: null)
        NULL_VALUE_IGNORED, // will accept a null value but won't encode it
    }

    private final Class<? extends T> javaClass;
    private final String codecId;

    public SerialCodecBase(Class<? extends T> javaClass, String codecId) {
        this.javaClass = javaClass;
        this.codecId = codecId;
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return javaClass;
    }

    @Override
    public String getCodecId() {
        return codecId;
    }

    protected boolean checkValue(String key, Object value, NullEncoding nullEncoding) {
        if (value != null)
            return true;
        if (nullEncoding == NullEncoding.NULL_VALUE_NOT_ALLOWED)
            throw new IllegalArgumentException("null value is not an allowed when serializing class = '" + javaClass + "', codedId = '" + codecId + "', key = '" + key + "'");
        return false;
    }

    protected void encodeObject(AstObject serial, String key, Object value) {
        encodeObject(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeObject(AstObject serial, String key, Object value, NullEncoding nullEncoding) {
        if (checkValue(key, value, nullEncoding))
            serial.set(key, SerialCodecManager.encodeToJson(value));
    }

    protected <T> T decodeObject(ReadOnlyAstObject serial, String key) {
        return decodeObject(serial, key, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected <T> T decodeObject(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        Object value = serial.get(key);
        if (checkValue(key, value, nullEncoding))
            return SerialCodecManager.decodeFromJson(value);
        return null;
    }

    protected void encodeString(AstObject serial, String key, String value) {
        encodeString(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeString(AstObject serial, String key, String value, NullEncoding nullEncoding) {
        if (checkValue(key, value, nullEncoding))
            serial.set(key, value);
    }

    protected String decodeString(ReadOnlyAstObject serial, String key) {
        return decodeString(serial, key, (String) null);
    }

    protected String decodeString(ReadOnlyAstObject serial, String key, String defaultValue) {
        return decodeString(serial, key, defaultValue, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected String decodeString(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        return decodeString(serial, key, null, nullEncoding);
    }

    protected String decodeString(ReadOnlyAstObject serial, String key, String defaultValue, NullEncoding nullEncoding) {
        String value = serial.getString(key);
        if (checkValue(key, value, nullEncoding))
            return value;
        return defaultValue;
    }

    protected void encodeInteger(AstObject serial, String key, Integer value) {
        encodeInteger(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeInteger(AstObject serial, String key, Integer value, NullEncoding nullEncoding) {
        if (checkValue(key, value, nullEncoding))
            serial.set(key, value);
    }

    protected Integer decodeInteger(ReadOnlyAstObject serial, String key) {
        return decodeInteger(serial, key, (Integer) null);
    }

    protected Integer decodeInteger(ReadOnlyAstObject serial, String key, Integer defaultValue) {
        return decodeInteger(serial, key, defaultValue, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected Integer decodeInteger(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        return decodeInteger(serial, key, null, nullEncoding);
    }

    protected Integer decodeInteger(ReadOnlyAstObject serial, String key, Integer defaultValue, NullEncoding nullEncoding) {
        Integer value = serial.getInteger(key);
        if (checkValue(key, value, nullEncoding))
            return value;
        return defaultValue;
    }

    protected void encodeLong(AstObject serial, String key, Long value) {
        encodeLong(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeLong(AstObject serial, String key, Long value, NullEncoding nullEncoding) {
        if (checkValue(key, value, nullEncoding))
            serial.set(key, value);
    }

    protected Long decodeLong(ReadOnlyAstObject serial, String key) {
        return decodeLong(serial, key, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected Long decodeLong(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        Long value = serial.getLong(key);
        if (checkValue(key, value, nullEncoding))
            return value;
        return null;
    }

    protected void encodeBoolean(AstObject serial, String key, Boolean value) {
        encodeBoolean(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeBoolean(AstObject serial, String key, Boolean value, NullEncoding nullEncoding) {
        if (checkValue(key, value, nullEncoding))
            serial.set(key, value);
    }

    protected Boolean decodeBoolean(ReadOnlyAstObject serial, String key) {
        return decodeBoolean(serial, key, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected Boolean decodeBoolean(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        Boolean value = serial.getBoolean(key);
        if (checkValue(key, value, nullEncoding))
            return value;
        return null;
    }

    protected void encodeInstant(AstObject serial, String key, Instant value) {
        encodeInstant(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeInstant(AstObject serial, String key, Instant value, NullEncoding nullEncoding) {
        if (checkValue(key, value, nullEncoding))
            serial.set(key, SerialCodecManager.encodeInstant(value));
    }

    protected Instant decodeInstant(ReadOnlyAstObject serial, String key) {
        return decodeInstant(serial, key, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected Instant decodeInstant(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        String value = serial.getString(key);
        if (checkValue(key, value, nullEncoding))
            return SerialCodecManager.decodeInstant(value);
        return null;
    }

    protected void encodeLocalDate(AstObject serial, String key, LocalDate value) {
        encodeLocalDate(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeLocalDate(AstObject serial, String key, LocalDate value, NullEncoding nullEncoding) {
        if (checkValue(key, value, nullEncoding))
            serial.set(key, SerialCodecManager.encodeLocalDate(value));
    }


    protected LocalDate decodeLocalDate(ReadOnlyAstObject serial, String key) {
        return decodeLocalDate(serial, key, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected LocalDate decodeLocalDate(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        String value = serial.getString(key);
        if (checkValue(key, value, nullEncoding))
            return SerialCodecManager.decodeLocalDate(value);
        return null;
    }

    protected void encodeLocalDateTime(AstObject serial, String key, LocalDateTime value) {
        encodeLocalDateTime(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeLocalDateTime(AstObject serial, String key, LocalDateTime value, NullEncoding nullEncoding) {
        if (checkValue(key, value, nullEncoding))
            serial.set(key, SerialCodecManager.encodeLocalDateTime(value));
    }

    protected LocalDateTime decodeLocalDateTime(ReadOnlyAstObject serial, String key) {
        return decodeLocalDateTime(serial, key, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected LocalDateTime decodeLocalDateTime(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        String value = serial.getString(key);
        if (checkValue(key, value, nullEncoding))
            return SerialCodecManager.decodeLocalDateTime(value);
        return null;
    }

    protected void encodeLocalTime(AstObject serial, String key, LocalTime value) {
        encodeLocalTime(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeLocalTime(AstObject serial, String key, LocalTime value, NullEncoding nullEncoding) {
        if (checkValue(key, value, nullEncoding))
            serial.set(key, SerialCodecManager.encodeLocalTime(value));
    }

    protected LocalTime decodeLocalTime(ReadOnlyAstObject serial, String key) {
        return decodeLocalTime(serial, key, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected LocalTime decodeLocalTime(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        String value = serial.getString(key);
        if (checkValue(key, value, nullEncoding))
            return SerialCodecManager.decodeLocalTime(value);
        return null;
    }

    // Arrays

    protected <T> void encodeArray(AstObject serial, String key, T[] value) {
        encodeArray(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected <T> void encodeArray(AstObject serial, String key, T[] value, NullEncoding nullEncoding) {
        if (checkValue(key, value, nullEncoding))
            serial.set(key, SerialCodecManager.encodeJavaArrayToAstArray(value));
    }

    protected <T> T[] decodeArray(ReadOnlyAstObject serial, String key) {
        return decodeArray(serial, key, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected <T> T[] decodeArray(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        return decodeArray(serial, key, null, nullEncoding);
    }

    protected <T> T[] decodeArray(ReadOnlyAstObject serial, String key, Class<T> expectedClass) {
        return decodeArray(serial, key, expectedClass, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected <T> T[] decodeArray(ReadOnlyAstObject serial, String key, Class<T> expectedClass, NullEncoding nullEncoding) {
        ReadOnlyAstArray value = serial.getArray(key);
        if (checkValue(key, value, nullEncoding))
            return SerialCodecManager.decodeAstArrayToJavaArray(value, expectedClass);
        return null;
    }

    protected void encodeObjectArray(AstObject serial, String key, Object[] value) {
        encodeObjectArray(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeObjectArray(AstObject serial, String key, Object[] value, NullEncoding nullEncoding) {
        encodeArray(serial, key, value, nullEncoding);
    }

    protected Object[] decodeObjectArray(ReadOnlyAstObject serial, String key) {
        return decodeObjectArray(serial, key, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected Object[] decodeObjectArray(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        return decodeArray(serial, key, Object.class, nullEncoding);
    }

    protected void encodeStringArray(AstObject serial, String key, String[] value) {
        encodeArray(serial, key, value, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected void encodeStringArray(AstObject serial, String key, String[] value, NullEncoding nullEncoding) {
        encodeArray(serial, key, value, nullEncoding);
    }

    protected String[] decodeStringArray(ReadOnlyAstObject serial, String key) {
        return decodeStringArray(serial, key, NullEncoding.NULL_VALUE_IGNORED);
    }

    protected String[] decodeStringArray(ReadOnlyAstObject serial, String key, NullEncoding nullEncoding) {
        return decodeArray(serial, key, String.class, nullEncoding);
    }

}
