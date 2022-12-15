package dev.webfx.stack.authn.server.gateway.spi.mojoauth;

import com.mojoauth.sdk.api.MojoAuthApi;
import com.mojoauth.sdk.models.responsemodels.UserResponse;
import com.mojoauth.sdk.models.responsemodels.VerifyTokenResponse;
import com.mojoauth.sdk.util.AsyncHandler;
import com.mojoauth.sdk.util.ErrorResponse;
import com.mojoauth.sdk.util.Jwks;
import com.mojoauth.sdk.util.MojoAuthSDK;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.json.ReadOnlyJsonObject;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.login.spi.impl.server.gateway.mojoauth.MojoAuthServerLoginGatewayConfigurationConsumer;
import dev.webfx.stack.authn.server.gateway.spi.impl.Jwt;
import dev.webfx.stack.authn.server.gateway.spi.impl.ServerAuthenticationGatewayProviderBase;

/**
 * @author Bruno Salmon
 */
public final class MojoAuthServerAuthenticationGatewayProvider extends ServerAuthenticationGatewayProviderBase {

    private final static String MOJO_AUTH_PREFIX = "MojoAuth.";
    //private final static String USERS_STATUS_URL = "https://api.mojoauth.com/users/status";

    private final MojoAuthApi mojoAuthApi;

    public MojoAuthServerAuthenticationGatewayProvider() {
        super(MOJO_AUTH_PREFIX);
        MojoAuthSDK.Initialize.setApiKey(MojoAuthServerLoginGatewayConfigurationConsumer.MOJO_AUTH_API_KEY);
        mojoAuthApi = new MojoAuthApi();
    }

    @Override
    protected void authenticateImpl(String stateId, Promise<String> promise) {
        mojoAuthApi.pingStatus(stateId, new AsyncHandler<>() {
            @Override
            public void onSuccess(UserResponse data) {
                if (data.getAuthenticated())
                    promise.complete(MOJO_AUTH_PREFIX + data.getOauth().getIdToken()); // Returning the ID token, so we can extract its claims in getUserClaims()
                else
                    promise.fail("User not authenticated");
            }

            @Override
            public void onFailure(ErrorResponse errorcode) {
                promise.fail("MojoAuth authentication failed: " + errorcode.getMessage());
            }
        });
    }

    @Override
    protected void getUserClaimsImpl(String oAuthIdToken, Promise<UserClaims> promise) {
        // Step 1) Verifying the passed JWT token
        Jwks jwks = new Jwks();
        jwks.verifyAccessToken(oAuthIdToken, new AsyncHandler<>() {
            @Override
            public void onSuccess(VerifyTokenResponse data) {
                if (!data.getIsValid())
                    promise.fail("Invalid JWT token");
                else { // Step 2) Decoding the JWT and extracting the claims
                    try {
                        Jwt jwt = new Jwt(oAuthIdToken);
                        ReadOnlyJsonObject payload = jwt.getJsonPayload();
                        UserClaims userClaims = new UserClaims(payload.getString("name"), payload.getString("email"), payload.getString("phone"), payload);
                        promise.complete(userClaims);
                    } catch (Exception e) {
                        promise.fail(e);
                    }
                }
            }

            @Override
            public void onFailure(ErrorResponse errorcode) {
                promise.fail(errorcode.getMessage());
            }
        });

/*
        mojoAuthApi.getJWKS(new AsyncHandler<>() {
            @Override
            public void onSuccess(JwksResponse data) {
                System.out.println("getJWKS() success! data = " + data);
            }

            @Override
            public void onFailure(ErrorResponse errorcode) {
                System.out.println("getJWKS() failure! errorcode = " + errorcode);
            }
        });
*/

/*
        UserResponse userResponse = (UserResponse) oAuthIdToken;
        String stateId = userResponse.getUser().getIssuer(); // See hack above in authenticate()
        return Fetch.fetch(USERS_STATUS_URL + "?state_id=" + stateId, new FetchOptions().setHeaders(Fetch.createHeaders()
                        .append("X-API-Key", API_KEY)))
                .compose(response -> response.jsonObject()
                        .map(json -> {
                            JsonObject user = json.getObject("user");
                            return new UserClaims(user.getString("name"), user.getString("email"), user.getString("phone"), user);
                        }));
*/
    }
}
