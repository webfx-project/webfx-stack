package dev.webfx.stack.auth.authn.spi.impl.mojoauth;

import com.mojoauth.sdk.api.MojoAuthApi;
import com.mojoauth.sdk.models.responsemodels.UserResponse;
import com.mojoauth.sdk.util.AsyncHandler;
import com.mojoauth.sdk.util.ErrorResponse;
import com.mojoauth.sdk.util.MojoAuthSDK;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.stack.auth.authn.spi.AuthenticationServiceProvider;

/**
 * @author Bruno Salmon
 */
public final class MojoAuthAuthenticationServiceProvider implements AuthenticationServiceProvider {

    private final static String API_KEY = "test-72827470-9205-4e4b-ab73-292fb871ba5c";

    @Override
    public Future<UserResponse> authenticate(Object userCredentials) {
        System.out.println("Checking MojoAuth token " + userCredentials);
        if (userCredentials == null)
            return Future.failedFuture("Wrong MojoAuth token must be non-null");
        String statusId = userCredentials.toString();
        Promise<UserResponse> promise = Promise.promise();
        MojoAuthSDK.Initialize init = new MojoAuthSDK.Initialize();
        init.setApiKey(API_KEY);
        MojoAuthApi mojoAuthApi = new MojoAuthApi();
        mojoAuthApi.pingStatus(statusId, new AsyncHandler<>() {
            @Override
            public void onSuccess(UserResponse data) {
                if (data.getAuthenticated())
                    promise.complete(data);
                else
                    promise.fail("MojoAuth authentication failed");
            }

            @Override
            public void onFailure(ErrorResponse errorcode) {
                promise.fail("MojoAuth authentication failure: " + errorcode.getMessage());
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
        Jwks jwks = new Jwks();
        jwks.verifyAccessToken(token, new AsyncHandler<>() {

            @Override
            public void onSuccess(VerifyTokenResponse data) {
                System.out.println(data.getAccessToken());
                System.out.println(data.getIsValid());
                if (data.getIsValid())
                    promise.complete(null);
                else
                    promise.fail("Invalid token!!!");
            }

            @Override
            public void onFailure(ErrorResponse errorcode) {
                System.out.println(errorcode.getMessage());
                System.out.println(errorcode.getDescription());
                promise.fail(new Exception(errorcode.getMessage()));
            }
        });
*/

        return promise.future();
    }

}
