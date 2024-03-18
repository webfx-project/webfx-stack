package dev.webfx.stack.com.serial.spi;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;

/*
 * @author Bruno Salmon
 */

public interface SerialCodec<T> {

    String getCodecId();

    Class<? extends T> getJavaClass();

    void encodeToJson(T javaObject, AstObject json);

    T decodeFromJson(ReadOnlyAstObject json);

}
