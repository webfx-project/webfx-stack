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
    public void encodeToJson(Instant value, AstObject json) {
        json.set(VALUE_KEY, value.toString());
    }

    @Override
    public Instant decodeFromJson(ReadOnlyAstObject json) {
        return Instant.parse(json.get(VALUE_KEY));
    }
}
