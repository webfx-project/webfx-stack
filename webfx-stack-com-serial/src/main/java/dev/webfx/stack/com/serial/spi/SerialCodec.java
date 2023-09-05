package dev.webfx.stack.com.serial.spi;

import dev.webfx.platform.ast.json.JsonObject;
import dev.webfx.platform.ast.json.ReadOnlyJsonObject;

/*
 * @author Bruno Salmon
 */

public interface SerialCodec<T> {

    String getCodecId();

    Class<? extends T> getJavaClass();

    void encodeToJson(T javaObject, JsonObject json);

    T decodeFromJson(ReadOnlyJsonObject json);

}
