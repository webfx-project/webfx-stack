package dev.webfx.stack.authn.buscall;

/**
 * @author Bruno Salmon
 */
public final class AuthenticationServiceBusAddress {

    public static final String AUTHENTICATE_METHOD_ADDRESS = "service/authn/authenticate";

    public static final String VERIFY_AUTHENTICATED_METHOD_ADDRESS = "service/authn/verifyAuthenticated";

    public static final String GET_USER_CLAIMS_METHOD_ADDRESS = "service/authn/getUserDetails";

    public static final String UPDATE_CREDENTIALS_METHOD_ADDRESS = "service/authn/updateCredentials";

    public static final String LOGOUT_METHOD_ADDRESS = "service/authn/logout";

}
