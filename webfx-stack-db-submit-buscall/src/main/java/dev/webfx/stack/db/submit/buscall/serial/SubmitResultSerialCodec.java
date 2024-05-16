package dev.webfx.stack.db.submit.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.Arrays;
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
    public void encode(SubmitResult arg, AstObject serial) {
        encodeInteger(        serial, ROW_COUNT_KEY,     arg.getRowCount());
        if (!Arrays.isEmpty(arg.getGeneratedKeys()))
            encodeObjectArray(serial, GENERATED_KEYS_KEY, arg.getGeneratedKeys());
    }

    @Override
    public SubmitResult decode(ReadOnlyAstObject serial) {
        return new SubmitResult(
                decodeInteger(    serial, ROW_COUNT_KEY),
                decodeObjectArray(serial, GENERATED_KEYS_KEY)
        );
    }
}
