package dev.webfx.stack.com.serial.spi.impl.time;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

import java.time.LocalDateTime;

/**
 * @author Bruno Salmon
 */
public final class LocalDateTimeSerialCodec extends SerialCodecBase<LocalDateTime> {

    private static final String CODEC_ID = "LocalDateTime";
    private static final String VALUE_KEY = "v";

    public LocalDateTimeSerialCodec() {
        super(LocalDateTime.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(LocalDateTime value, AstObject json) {
        json.set(VALUE_KEY, value.toString());
    }

    @Override
    public LocalDateTime decodeFromJson(ReadOnlyAstObject json) {
        return LocalDateTime.parse(json.get(VALUE_KEY));
    }
}
