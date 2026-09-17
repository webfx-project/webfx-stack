package dev.webfx.stack.session.token;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;

/**
 * Encodes {@link CheckPrincipal} the way an application encodes its own principal — same base class, same
 * shape of payload — so the checks exercise the real codec path rather than a shortcut around it.
 */
public final class CheckPrincipalSerialCodec extends SerialCodecBase<CheckPrincipal> {

    public static final String CODEC_ID = "CheckPrincipal";
    private static final String PERSON_ID_KEY = "personId";
    private static final String ACCOUNT_ID_KEY = "accountId";

    public CheckPrincipalSerialCodec() {
        super(CheckPrincipal.class, CODEC_ID);
    }

    @Override
    public void encode(CheckPrincipal arg, AstObject serial) {
        encodeObject(serial, PERSON_ID_KEY, arg.getPersonId());
        encodeObject(serial, ACCOUNT_ID_KEY, arg.getAccountId());
    }

    @Override
    public CheckPrincipal decode(ReadOnlyAstObject serial) {
        return new CheckPrincipal(decodeObject(serial, PERSON_ID_KEY), decodeObject(serial, ACCOUNT_ID_KEY));
    }
}
