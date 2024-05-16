package dev.webfx.stack.com.serial.spi.impl.time;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

import java.time.Instant;

/**
 * @author Bruno Salmon
 */
public final class InstantSerialCodec extends SerialCodecBase<Instant> {

    private static final String CODEC_ID = "Instant";
    private static final String VALUE_KEY = "v";

    public InstantSerialCodec() {
        super(Instant.class, CODEC_ID);
    }

    @Override
    public void encode(Instant value, AstObject serial) {
        encodeInstant(serial, VALUE_KEY, value);
    }

    @Override
    public Instant decode(ReadOnlyAstObject serial) {
        return decodeInstant(serial, VALUE_KEY);
    }
}
