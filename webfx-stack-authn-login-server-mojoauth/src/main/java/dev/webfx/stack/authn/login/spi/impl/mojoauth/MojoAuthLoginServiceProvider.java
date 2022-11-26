package dev.webfx.stack.authn.login.spi.impl.mojoauth;

import dev.webfx.platform.async.Future;
import dev.webfx.stack.authn.login.spi.LoginServiceProvider;
import dev.webfx.stack.session.state.ThreadLocalStateHolder;

/**
 * @author Bruno Salmon
 */
public class MojoAuthLoginServiceProvider implements LoginServiceProvider {

    private final static String API_KEY = "test-72827470-9205-4e4b-ab73-292fb871ba5c";
    final static String REDIRECT_PATH = "/login/mojoAuth";
    private static final String HTML_TEMPLATE = "<!DOCTYPE html>\n" +
            "<head>\n" +
            "    <script charset='UTF-8' src='https://cdn.mojoauth.com/js/mojoauth.min.js'>\n" +
            "    </script>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div id='mojoauth-passwordless-form'></div>\n" +
            "<script>\n" +
            "\n" +
            "    const mojoauth = new MojoAuth('{{API_KEY}}', {\n" +
            "      language: 'en_GB',\n" +
            "      redirect_url: '{{RETURN_URL}}?sessionId={{SESSION_ID}}'," +
            "      source: [" +
            "       { type: 'email', feature: 'magiclink' } \n" +
//            "       { type: 'email', feature: 'otp' }, \n" +
//            "       { type: 'phone', feature: 'otp' } \n" +
            "       ]});\n" +
            "\n" +
            "    mojoauth.signIn().then(response => console.log(response));\n" +
            "\n" +
            "</script>\n" +
            "</body>\n" +
            "</html>\n";

    @Override
    public Future<?> getLoginUiInput() {
        String serverSessionId = ThreadLocalStateHolder.getServerSessionId();
        String RETURN_URL = "http://127.0.0.1:8080" + REDIRECT_PATH;
        String html = HTML_TEMPLATE
                .replace("{{API_KEY}}", API_KEY)
                .replace("{{RETURN_URL}}", RETURN_URL)
                .replace("{{SESSION_ID}}", serverSessionId);
        return Future.succeededFuture(html);
    }

}
