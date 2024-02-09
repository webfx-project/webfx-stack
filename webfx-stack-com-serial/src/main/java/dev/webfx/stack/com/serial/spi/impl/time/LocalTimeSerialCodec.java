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
    public void encodeToJson(LocalTime value, AstObject json) {
        json.set(VALUE_KEY, value.toString());
    }

    @Override
    public LocalTime decodeFromJson(ReadOnlyAstObject json) {
        return LocalTime.parse(json.get(VALUE_KEY));
    }
}
