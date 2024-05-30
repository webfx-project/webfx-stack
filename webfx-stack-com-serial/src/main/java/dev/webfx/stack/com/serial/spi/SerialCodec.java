package dev.webfx.stack.com.serial.spi;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;

/*
 * @author Bruno Salmon
 */

public interface SerialCodec<T> {

    String getCodecId();

    Class<? extends T> getJavaClass();

    void encode(T javaObject, AstObject serial);

    T decode(ReadOnlyAstObject serial);

}
