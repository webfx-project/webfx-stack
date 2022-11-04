package dev.webfx.stack.db.submit.buscall.serial;

import dev.webfx.platform.json.JsonObject;
import dev.webfx.platform.json.WritableJsonObject;
import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.com.serial.SerialCodecManager;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.submit.SubmitResult;

public final class SubmitResultSerialCodec extends SerialCodecBase<SubmitResult> {

    private final static String CODEC_ID = "SubmitResult";
    private final static String ROW_COUNT_KEY = "rows";
    private final static String GENERATED_KEYS_KEY = "genKeys";

    public SubmitResultSerialCodec() {
        super(SubmitResult.class, CODEC_ID);
    }

    @Override
    public void encodeToJson(SubmitResult arg, WritableJsonObject json) {
        json.set(ROW_COUNT_KEY, arg.getRowCount());
        if (!Arrays.isEmpty(arg.getGeneratedKeys()))
            json.set(GENERATED_KEYS_KEY, SerialCodecManager.encodePrimitiveArrayToJsonArray(arg.getGeneratedKeys()));
    }

    @Override
    public SubmitResult decodeFromJson(JsonObject json) {
        return new SubmitResult(
                json.getInteger(ROW_COUNT_KEY),
                SerialCodecManager.decodePrimitiveArrayFromJsonArray(json.getArray(GENERATED_KEYS_KEY))
        );
    }
}
