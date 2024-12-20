package dev.webfx.stack.db.submit.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.submit.GeneratedKeyReference;

public final class GeneratedKeyReferenceSerialCodec extends SerialCodecBase<GeneratedKeyReference> {

    private static final String CODEC_ID = "GeneratedKeyReference";
    private static final String STATEMENT_BATCH_INDEX_KEY = "batch";
    private static final String GENERATED_KEY_INDEX_KEY = "key";

    public GeneratedKeyReferenceSerialCodec() {
        super(GeneratedKeyReference.class, CODEC_ID);
    }

    @Override
    public void encode(GeneratedKeyReference arg, AstObject serial) {
        encodeInteger(serial, STATEMENT_BATCH_INDEX_KEY, arg.getStatementBatchIndex());
        encodeInteger(serial, GENERATED_KEY_INDEX_KEY, arg.getGeneratedKeyIndex(), 0);
    }

    @Override
    public GeneratedKeyReference decode(ReadOnlyAstObject serial) {
        return new GeneratedKeyReference(
                decodeInteger(serial, STATEMENT_BATCH_INDEX_KEY),
                decodeInteger(serial, GENERATED_KEY_INDEX_KEY, 0)
        );
    }
}
