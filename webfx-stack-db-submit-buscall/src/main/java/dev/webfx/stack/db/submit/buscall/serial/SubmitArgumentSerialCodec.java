package dev.webfx.stack.db.submit.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.submit.SubmitArgument;

public final class SubmitArgumentSerialCodec extends SerialCodecBase<SubmitArgument> {

    private static final String CODEC_ID = "SubmitArgument";
    private static final String DATA_SOURCE_ID_KEY = "dataSourceId";
    private static final String DATA_SCOPE_KEY = "dataScope";
    private static final String RETURN_GENERATED_KEYS_KEY = "genKeys";
    private static final String LANGUAGE_KEY = "lang";
    private static final String STATEMENT_KEY = "statement";
    private static final String PARAMETERS_KEY = "parameters";

    public SubmitArgumentSerialCodec() {
        super(SubmitArgument.class, CODEC_ID);
    }

    @Override
    public void encode(SubmitArgument arg, AstObject serial) {
        encodeObject(        serial, DATA_SOURCE_ID_KEY,        arg.getDataSourceId());
        encodeObject(        serial, DATA_SCOPE_KEY,            arg.getDataScope());
        encodeBoolean(       serial, RETURN_GENERATED_KEYS_KEY, arg.returnGeneratedKeys());
        encodeString(        serial, LANGUAGE_KEY,              arg.getLanguage());
        encodeString(        serial, STATEMENT_KEY,             arg.getStatement());
        if (!Arrays.isEmpty(arg.getParameters()))
            encodeObjectArray(serial, PARAMETERS_KEY,           arg.getParameters());
    }

    @Override
    public SubmitArgument decode(ReadOnlyAstObject serial) {
        return new SubmitArgument(null,
                decodeObject(     serial, DATA_SOURCE_ID_KEY),
                decodeObject(     serial, DATA_SCOPE_KEY),
                decodeBoolean(    serial, RETURN_GENERATED_KEYS_KEY),
                decodeString(     serial, LANGUAGE_KEY),
                decodeString(     serial, STATEMENT_KEY),
                decodeObjectArray(serial, PARAMETERS_KEY)
        );
    }
}
