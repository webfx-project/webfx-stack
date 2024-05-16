package dev.webfx.stack.db.submit.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.submit.GeneratedKeyBatchIndex;

public final class GeneratedKeyBatchIndexSerialCodec extends SerialCodecBase<GeneratedKeyBatchIndex> {

    private static final String CODEC_ID = "GenKeyBatchIndex";
    private static final String BATCH_INDEX_KEY = "index";

    public GeneratedKeyBatchIndexSerialCodec() {
        super(GeneratedKeyBatchIndex.class, CODEC_ID);
    }

    @Override
    public void encode(GeneratedKeyBatchIndex arg, AstObject serial) {
        encodeInteger(serial, BATCH_INDEX_KEY, arg.getBatchIndex());
    }

    @Override
    public GeneratedKeyBatchIndex decode(ReadOnlyAstObject serial) {
        return new GeneratedKeyBatchIndex(
                decodeInteger(serial, BATCH_INDEX_KEY)
        );
    }
}
