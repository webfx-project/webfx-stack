package dev.webfx.stack.authn.server.gateway.spi.impl.facebook;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.async.Promise;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.fetch.json.JsonFetch;
import dev.webfx.stack.authn.UserClaims;
import dev.webfx.stack.authn.server.gateway.spi.impl.ServerAuthenticationGatewayBase;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

import static dev.webfx.stack.authn.login.spi.impl.server.gateway.facebook.FacebookServerLoginGateway.*;

/**
 * @author Bruno Salmon
 */
public final class FacebookServerAuthenticationGateway extends ServerAuthenticationGatewayBase {

    private final static String FACEBOOK_AUTH_TOKEN_PREFIX = "Facebook.";
    private static final String FB_JSON_API_APP_TOKEN_URL_TEMPLATE = "https://graph.facebook.com/oauth/access_token?client_id={{CLIENT_ID}}&client_secret={{CLIENT_SECRET}}&grant_type=client_credentials";
    private static final String FB_JSON_API_USER_TOKEN_URL_TEMPLATE = "https://graph.facebook.com/v15.0/oauth/access_token?client_id={{CLIENT_ID}}&redirect_uri={{RETURN_URL}}?state={{SESSION_ID}}&client_secret={{CLIENT_SECRET}}&code={{USER_CODE}}";
    private static final String FB_JSON_API_INSPECT_TOKEN_URL_TEMPLATE = "https://graph.facebook.com/debug_token?input_token={{ACCESS_TOKEN}}&access_token={{APP_ACCESS_TOKEN}}";
    private static final String FB_JSON_API_USER_DETAILS_URL_TEMPLATE = "https://graph.facebook.com/me?fields=email,name&access_token={{ACCESS_TOKEN}}";

    private String appAccessToken;

    public FacebookServerAuthenticationGateway() {
        super(FACEBOOK_AUTH_TOKEN_PREFIX);
    }

    @Override
    public void boot() {
        onValidConfig(() -> {
            // Getting the "APP" access token (for further use in the FB API)
            fetchJson(resolveTemplateVariables(FB_JSON_API_APP_TOKEN_URL_TEMPLATE))
                    .onFailure(e -> Console.log("❌ Error while getting the Facebook application access token: " + e.getMessage()))
                    .onSuccess(json -> {
                        appAccessToken = json.getString("access_token");
                        if (appAccessToken == null)
                            Console.log("❌ Facebook returned a null application access token!");
                        else
                            Console.log("✅ Successfully received Facebook application access token");
                    });
        });
    }

    @Override
    protected void authenticateImpl(String code, Promise<String> promise) {
        fetchJson(resolveTemplateVariablesWithUserCode(FB_JSON_API_USER_TOKEN_URL_TEMPLATE, code))
                .onFailure(promise::fail)
                .onSuccess(json -> promise.complete(FACEBOOK_AUTH_TOKEN_PREFIX + json.getString("access_token")));
    }

    @Override
    protected void getUserClaimsImpl(String userAccessToken, Promise<UserClaims> promise) {
        fetchJson(resolveTemplateVariablesWithAccessToken(FB_JSON_API_USER_DETAILS_URL_TEMPLATE, userAccessToken))
                .onFailure(promise::fail)
                .onSuccess(userDetailsJson -> promise.complete(new UserClaims(
                        userDetailsJson.get("name"),
                        userDetailsJson.get("email"),
                        userDetailsJson.get("phone"),
                        userDetailsJson
                )));
    }

    private String resolveTemplateVariables(String template) {
        return template
                .replace("{{CLIENT_ID}}", FACEBOOK_CLIENT_ID)
                .replace("{{CLIENT_SECRET}}", FACEBOOK_CLIENT_SECRET)
                .replace("{{RETURN_URL}}", REDIRECT_ORIGIN + REDIRECT_PATH)
                ;
    }

    private String resolveTemplateVariablesWithUserCode(String template, String userCode) {
        return resolveTemplateVariables(template)
                .replace("{{USER_CODE}}", userCode)
                .replace("{{SESSION_ID}}", ThreadLocalStateHolder.getServerSessionId())
                ;
    }

    private String resolveTemplateVariablesWithAccessToken(String template, String accessToken) {
        return resolveTemplateVariables(template)
                .replace("{{ACCESS_TOKEN}}", accessToken)
                .replace("{{APP_ACCESS_TOKEN}}", appAccessToken)
                ;
    }

    private static Future<ReadOnlyAstObject> fetchJson(String url) {
        // Console.log("Fetching " + url); // Uncomment for debug, but be careful as this will reveal the client secret in the logs
        return JsonFetch.fetchJsonObject(url)
                .compose(json -> {
                    ReadOnlyAstObject error = json.getObject("error");
                    if (error != null)
                        return Future.failedFuture(error.getString("message"));
                    return Future.succeededFuture(json);
                });
    }

}
