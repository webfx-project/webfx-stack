package dev.webfx.stack.db.submit.buscall.serial;

import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.json.WritableJsonObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.submit.GeneratedKeyBatchIndex;

public final class GeneratedKeyBatchIndexSerialCodec extends SerialCodecBase<GeneratedKeyBatchIndex> {

    private static final String CODEC_ID = "GenKeyBatchIndex";
    private static final String BATCH_INDEX_KEY = "index";

    public GeneratedKeyBatchIndexSerialCodec() {
        super(GeneratedKeyBatchIndex.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(GeneratedKeyBatchIndex arg, WritableJsonObject json) {
        json.set(BATCH_INDEX_KEY, arg.getBatchIndex());
    }

    @Override
    public GeneratedKeyBatchIndex decodeFromJson(JsonObject json) {
        return new GeneratedKeyBatchIndex(
                json.getInteger(BATCH_INDEX_KEY)
        );
    }
}
