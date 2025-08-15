package dev.webfx.stack.db.query.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.query.QueryArgument;

public final class QueryArgumentSerialCodec extends SerialCodecBase<QueryArgument> {

    private static final String CODEC_ID = "QueryArgument";
    private static final String DATA_SOURCE_ID_KEY = "dataSourceId";
    private static final String DATA_SCOPE_KEY = "dataScope";
    private static final String LANGUAGE_KEY = "lang";
    private static final String STATEMENT_KEY = "statement";
    private static final String PARAMETERS_KEY = "parameters";
    private static final String PARAMETER_NAMES_KEY = "names";

    public QueryArgumentSerialCodec() {
        super(QueryArgument.class, CODEC_ID);
    }

    @Override
    public void encode(QueryArgument arg, AstObject serial) {
        encodeObject(         serial, DATA_SOURCE_ID_KEY,  arg.getDataSourceId());
        encodeObject(         serial, DATA_SCOPE_KEY,      arg.getDataScope());
        encodeString(         serial, LANGUAGE_KEY,        arg.getLanguage());
        encodeString(         serial, STATEMENT_KEY,       arg.getStatement());
        if (!Arrays.isEmpty(arg.getParameters()))
            encodeObjectArray(serial, PARAMETERS_KEY,      arg.getParameters());
        if (!Arrays.isEmpty(arg.getParameterNames()))
            encodeObjectArray(serial, PARAMETER_NAMES_KEY, arg.getParameterNames());
    }

    @Override
    public QueryArgument decode(ReadOnlyAstObject serial) {
        return new QueryArgument(null,
            decodeObject(serial,      DATA_SOURCE_ID_KEY),
            decodeObject(serial,      DATA_SCOPE_KEY),
            decodeString(serial,      LANGUAGE_KEY),
            decodeString(serial,      STATEMENT_KEY),
            decodeObjectArray(serial, PARAMETERS_KEY),
            decodeStringArray(serial, PARAMETER_NAMES_KEY)
        );
    }
}
