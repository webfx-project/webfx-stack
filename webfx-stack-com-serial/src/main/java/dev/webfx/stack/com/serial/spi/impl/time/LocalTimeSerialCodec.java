package dev.webfx.stack.com.serial.spi.impl.time;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

import java.time.LocalTime;

/**
 * @author Bruno Salmon
 */
public final class LocalTimeSerialCodec extends SerialCodecBase<LocalTime> {

    private static final String CODEC_ID = "LocalTime";
    private static final String VALUE_KEY = "v";

    public LocalTimeSerialCodec() {
        super(LocalTime.class, CODEC_ID);
    }

    @Override
    public void encode(LocalTime value, AstObject serial) {
        encodeLocalTime(serial, VALUE_KEY, value);
    }

    @Override
    public LocalTime decode(ReadOnlyAstObject serial) {
        return decodeLocalTime(serial, VALUE_KEY);
    }
}
