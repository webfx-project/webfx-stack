package dev.webfx.stack.com.serial.spi.impl;

import dev.webfx.platform.ast.ReadOnlyAstArray;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.stack.com.serial.SerialCodecManager;
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
    public void encodeToJson(Batch batch, AstObject json) {
        json.set(BATCH_ARRAY_KEY, SerialCodecManager.encodeToAstArray(batch.getArray()));
    }

    @Override
    public Batch decodeFromJson(ReadOnlyAstObject json) {
        ReadOnlyAstArray array = json.getArray(BATCH_ARRAY_KEY);
        Class contentClass = Object.class;
        if (array.size() > 0)
            contentClass = SerialCodecManager.getJavaClass(array.getObject(0).getString(SerialCodecManager.CODEC_ID_KEY));
        return new Batch<>(SerialCodecManager.decodeFromAstArray(array, contentClass));
    }
}
