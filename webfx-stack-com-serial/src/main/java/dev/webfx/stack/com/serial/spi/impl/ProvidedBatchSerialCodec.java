package dev.webfx.stack.com.serial.spi.impl;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Batch;

/**
 * @author Bruno Salmon
 */
public final class ProvidedBatchSerialCodec extends SerialCodecBase<Batch> {

    private final static String BATCH_CODEC_ID = "Batch";
    private final static String BATCH_ARRAY_KEY = "array";

    public ProvidedBatchSerialCodec() {
        super(Batch.class, BATCH_CODEC_ID);
    }

    @Override
    public void encode(Batch batch, AstObject serial) {
        encodeArray(serial, BATCH_ARRAY_KEY, batch.getArray());
    }

    @Override
    public Batch decode(ReadOnlyAstObject serial) {
        return new Batch<>(
                decodeArray(serial, BATCH_ARRAY_KEY)
        );
    }
}
