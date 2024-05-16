package dev.webfx.stack.com.serial.spi.impl.time;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

import java.time.LocalDate;

/**
 * @author Bruno Salmon
 */
public final class LocalDateSerialCodec extends SerialCodecBase<LocalDate> {

    private static final String CODEC_ID = "LocalDate";
    private static final String VALUE_KEY = "v";

    public LocalDateSerialCodec() {
        super(LocalDate.class, CODEC_ID);
    }

    @Override
    public void encode(LocalDate value, AstObject serial) {
        encodeLocalDate(serial, VALUE_KEY, value);
    }

    @Override
    public LocalDate decode(ReadOnlyAstObject serial) {
        return decodeLocalDate(serial, VALUE_KEY);
    }
}
