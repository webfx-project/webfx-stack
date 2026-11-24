package dev.webfx.stack.db.querypush.buscall.serial;

import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.com.serial.spi.impl.SerialCodecBase;
import dev.webfx.stack.db.querypush.SessionSecurityMonitorInfo;

public final class SessionSecurityMonitorInfoSerialCodec extends SerialCodecBase<SessionSecurityMonitorInfo> {

    private static final String CODEC_ID = "SessionSecurityMonitorInfo";
    // Wire keys MUST match the client (@kbs3/shared) field names — the React client reads them by name
    // off the decoded payload, with no remapping. The lifetime scale is carried as a per-mille integer
    // (1000 = unscaled) for the same reason CPU load is: SerialCodecBase has no double primitive.
    private static final String REVOKED_FAMILIES_HELD_KEY = "revokedFamiliesHeld";
    private static final String REVOCATIONS_REFUSED_KEY = "revocationsRefused";
    private static final String LAST_POLL_AGE_MILLIS_KEY = "lastRevocationPollAgeMillis";
    private static final String TOKEN_REQUIRED_KEY = "tokenRequired";
    private static final String LIFETIME_SCALE_PERMILLE_KEY = "sessionLifetimeScalePermille";
    private static final String STORE_REGISTERED_KEY = "revocationStoreRegistered";

    public SessionSecurityMonitorInfoSerialCodec() {
        super(SessionSecurityMonitorInfo.class, CODEC_ID);
    }

    @Override
    public void encode(SessionSecurityMonitorInfo arg, AstObject serial) {
        encodeInteger(serial, REVOKED_FAMILIES_HELD_KEY, arg.getRevokedFamiliesHeld());
        encodeLong(   serial, REVOCATIONS_REFUSED_KEY,   arg.getRevocationsRefused());
        encodeLong(   serial, LAST_POLL_AGE_MILLIS_KEY,  arg.getLastRevocationPollAgeMillis());
        encodeBoolean(serial, TOKEN_REQUIRED_KEY,        arg.isTokenRequired());
        encodeInteger(serial, LIFETIME_SCALE_PERMILLE_KEY,
            (int) Math.round(arg.getSessionLifetimeScale() * 1000));
        encodeBoolean(serial, STORE_REGISTERED_KEY,      arg.isRevocationStoreRegistered());
    }

    @Override
    public SessionSecurityMonitorInfo decode(ReadOnlyAstObject serial) {
        // decodeLong has no default-value overload, and absent is what an older server sends: no
        // refusals counted, and a poll age of -1 meaning "this instance has never heard from the store".
        Long refused = decodeLong(serial, REVOCATIONS_REFUSED_KEY);
        Long pollAge = decodeLong(serial, LAST_POLL_AGE_MILLIS_KEY);
        return new SessionSecurityMonitorInfo(
                decodeInteger(serial, REVOKED_FAMILIES_HELD_KEY, 0),
                refused == null ? 0 : refused,
                pollAge == null ? -1 : pollAge,
                decodeBoolean(serial, TOKEN_REQUIRED_KEY, false),
                decodeInteger(serial, LIFETIME_SCALE_PERMILLE_KEY, 1000) / 1000d,
                decodeBoolean(serial, STORE_REGISTERED_KEY, false)
        );
    }
}
