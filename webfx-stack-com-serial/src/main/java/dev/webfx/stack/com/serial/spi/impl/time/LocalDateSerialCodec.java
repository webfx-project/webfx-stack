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
    public void encodeToJson(LocalDate value, AstObject json) {
        json.set(VALUE_KEY, value.toString());
    }

    @Override
    public LocalDate decodeFromJson(ReadOnlyAstObject json) {
        return LocalDate.parse(json.get(VALUE_KEY));
    }
}
