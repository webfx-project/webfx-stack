package dev.webfx.stack.com.serial.spi;

import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.json.WritableJsonObject;

/*
 * @author Bruno Salmon
 */

public interface SerialCodec<T> {

    String getCodecId();

    Class<? extends T> getJavaClass();

    void encodeToJson(T javaObject, WritableJsonObject json);

    T decodeFromJson(JsonObject json);

}
