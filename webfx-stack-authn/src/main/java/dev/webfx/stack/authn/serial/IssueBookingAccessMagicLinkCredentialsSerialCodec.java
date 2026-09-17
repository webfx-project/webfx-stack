package dev.webfx.stack.authn.serial;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.stack.authn.IssueBookingAccessMagicLinkCredentials;

/**
 * Wire codec for {@link IssueBookingAccessMagicLinkCredentials}.
 *
 * @author Bruno Salmon
 */
public final class IssueBookingAccessMagicLinkCredentialsSerialCodec extends AlternativeLoginActionCredentialsSerialCodec<IssueBookingAccessMagicLinkCredentials> {

    private static final String CODEC_ID = "IssueBookingAccessMagicLinkCredentials";

    public IssueBookingAccessMagicLinkCredentialsSerialCodec() {
        super(IssueBookingAccessMagicLinkCredentials.class, CODEC_ID);
    }

    @Override
    public IssueBookingAccessMagicLinkCredentials decode(ReadOnlyAstObject serial) {
        return new IssueBookingAccessMagicLinkCredentials(
            decodeString(serial,  EMAIL_KEY),
            decodeString(serial,  CLIENT_ORIGIN_KEY),
            decodeString(serial,  REQUESTED_PATH_KEY),
            decodeObject(serial,  LANGUAGE_KEY),
            decodeBoolean(serial, VERIFICATION_CODE_ONLY_KEY),
            decodeObject(serial,  CONTEXT_KEY)
        );
    }
}
