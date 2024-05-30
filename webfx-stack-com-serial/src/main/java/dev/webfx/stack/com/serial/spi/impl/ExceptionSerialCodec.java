package dev.webfx.stack.com.serial.spi.impl;

import dev.webfx.platform.console.Console;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.AstObject;

/**
 * @author Bruno Salmon
 */
public class ExceptionSerialCodec extends SerialCodecBase<Exception> {

    private static final String CODEC_ID = "exception";
    private static final String MESSAGE_KEY = "message";

    public ExceptionSerialCodec() {
        super(Exception.class, CODEC_ID);
    }

    @Override
    public void encode(Exception exception, AstObject serial) {
        encodeString(serial, MESSAGE_KEY, exception.getClass().getName() + ": " + exception.getMessage() + "\n" + Console.captureStackTrace(exception));
    }

    @Override
    public Exception decode(ReadOnlyAstObject serial) {
        return new Exception(
                decodeString(serial, MESSAGE_KEY)
        );
    }
}
