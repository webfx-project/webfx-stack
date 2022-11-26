package dev.webfx.stack.authn.spi.impl.mojoauth;

import com.mojoauth.sdk.api.MojoAuthApi;
import com.mojoauth.sdk.models.responsemodels.UserResponse;
import com.mojoauth.sdk.models.responsemodels.VerifyTokenResponse;
import com.mojoauth.sdk.util.AsyncHandler;
import com.mojoauth.sdk.util.ErrorResponse;
import com.mojoauth.sdk.util.Jwks;
import com.mojoauth.sdk.util.MojoAuthSDK;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.json.ReadOnlyJsonObject;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.spi.AuthenticationServiceProvider;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;
import dev.webfx.stack.session.state.server.ServerSideStateSessionSyncer;

/**
 * @author Bruno Salmon
 */
public final class MojoAuthAuthenticationServiceProvider implements AuthenticationServiceProvider {

    private final static String API_KEY = "test-72827470-9205-4e4b-ab73-292fb871ba5c";
    //private final static String USERS_STATUS_URL = "https://api.mojoauth.com/users/status";

    private final MojoAuthApi mojoAuthApi;

    public MojoAuthAuthenticationServiceProvider() {
        MojoAuthSDK.Initialize.setApiKey(API_KEY);
        mojoAuthApi = new MojoAuthApi();
    }

    @Override
    public Future<String/* OAuth JWT ID token */> authenticate(Object userId /* expecting state_id from Mojo login */) {
        if (!(userId instanceof String))
            return Future.failedFuture("MojoAuth authenticate() expects a String as input, not a " + (userId == null ? " null" : userId.getClass()) + " object.");
        String statusId = (String) userId;
        Promise<String> promise = Promise.promise();
        mojoAuthApi.pingStatus(statusId, new AsyncHandler<>() {
            @Override
            public void onSuccess(UserResponse data) {
                if (data.getAuthenticated())
                    promise.complete(data.getOauth().getIdToken()); // Returning the ID token, so we can extract its claims in getUserClaims()
                else
                    promise.fail("User not authenticated");
            }

            @Override
            public void onFailure(ErrorResponse errorcode) {
                promise.fail("MojoAuth authentication failed: " + errorcode.getMessage());
            }
        });
        return promise.future();
    }

    @Override
    public Future<?> verifyAuthenticated() {
        String userId = ThreadLocalStateHolder.getUserId();
        return getUserClaims().map(ignored -> userId);
    }

    @Override
    public Future<UserClaims> getUserClaims() {
        String oAuthIdToken = ThreadLocalStateHolder.getUserId(); //
        Promise<UserClaims> promise = Promise.promise();
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
        return promise.future();

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

    @Override
    public Future<Void> logout() {
        return ServerSideStateSessionSyncer.pushLogoutMessageToClient();
    }
}
