package dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.impl.openjfx;

import dev.webfx.stack.authn.login.ui.spi.impl.gateway.webview.spi.LoginWebViewProvider;
import dev.webfx.stack.ui.controls.dialog.DialogCallback;
import dev.webfx.stack.ui.controls.dialog.DialogUtil;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Bruno Salmon
 */
public class FXLoginWebViewProvider implements LoginWebViewProvider {

    private DialogCallback popupDialogCallback;

    @Override
    public WebView createLoginWebView() {
        WebView webView = new WebView();
        WebEngine mainWebEngine = webView.getEngine();

        // Some SSO logins like Google display the login process in a popup window. The JavaFX web engine doesn't allow
        // popups by default, unless we set a handler that is responsible for their creation. So that's what we do:
        mainWebEngine.setCreatePopupHandler(p -> { // called when the SSO login code requires a popup
            // We create a second web view to display the content of that popup
            WebView popupWebView = new WebView();
            // Instead of creating a new window, we create just a modal dialog embed within the main web view container.
            // It will display that popup web view. We also memorize the dialog callback, so we can close it later, when
            // the SSO login will change the URL of the main web view (indicating the final success callback).
            popupDialogCallback = DialogUtil.showModalNodeInGoldLayout(new StackPane(popupWebView), (Pane) webView.getParent());
            // The JavaFX API requires to return the web engine of that popup web view, so that's what we do:
            return popupWebView.getEngine();
        });

        // When there is a state change on the main web engine, this can indicate the final success callback:
        mainWebEngine.getLoadWorker().stateProperty().addListener((ov,oldState,newState) -> {
/*
            String html = (String) mainWebEngine.executeScript("document.documentElement.outerHTML");
            System.out.println(html);
*/
            Document document = mainWebEngine.getDocument();
            Element headerNotices = document == null ? null : document.getElementById("header-notices");
            if (headerNotices != null)
                headerNotices.getParentNode().removeChild(headerNotices);

            if (popupDialogCallback != null) { // indicates that there was login popup dialog
                popupDialogCallback.closeDialog(); // we close that dialog, because this state change must indicate the success callback
                popupDialogCallback = null; // No need to do it again
            }

/*
            try {
                Map<String, List<String>> cookies = CookieHandler.getDefault().get(new URI(".facebook.com"), new HashMap<>());
                System.out.println("cookies = " + cookies);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
*/
        });


/*

        CookieManager manager = new CookieManager(new CookieStore() {

            private final Map<String, HttpCookie> httpOnlyCookies = new HashMap<>();

            public void add(URI uri, HttpCookie cookie) {
                System.out.println("Adding cookie " + cookie + " for " + uri);
                System.out.println("domain = " + cookie.getDomain());
                System.out.println("maxAge = " + cookie.getMaxAge());
                System.out.println("path = " + cookie.getPath());
                System.out.println("httpOnly = " + cookie.isHttpOnly());
                System.out.println("portList = " + cookie.getPortlist());
                System.out.println("discard = " + cookie.getDiscard());
                System.out.println("secure = " + cookie.getSecure());
                System.out.println("version = " + cookie.getVersion());
                System.out.println("comment = " + cookie.getComment());
                System.out.println("commentURL = " + cookie.getCommentURL());
                if (uri.toString().contains(".facebook.com")) {
                    if (cookie.isHttpOnly())
                        httpOnlyCookies.put(cookie.getName(), cookie);
                    else
                        LocalStorage.setItem(cookie.getName(), cookie.getValue());
                }
            }

            @Override
            public List<HttpCookie> get(URI uri) {
                System.out.println("Requesting cookies for " + uri);
                if (uri.toString().contains(".facebook.com"))
                    return createFBCookies();
                return List.of();
            }

            private List<HttpCookie> createFBCookies() {
                List<HttpCookie> cookies = new ArrayList<>();
                String[] keys = {"_js_datr", "datr", "c_user", "xs"};
                for (String key : keys) {
                    HttpCookie httpCookie = httpOnlyCookies.get(key);
                    if (httpCookie != null)
                        cookies.add(new HttpCookie(key, ""));
                    else {
                        String value = LocalStorage.getItem(key);
                        if (value != null)
                            cookies.add(createFBCookie(key, value));
                    }
                }
                return cookies;
            }

            private HttpCookie createFBCookie(String key, String value) {
                HttpCookie cookie = new HttpCookie(key, value);
                boolean deleted = "deleted".equals(value);
                cookie.setMaxAge(deleted ? 0 : 63071999);
                cookie.setDomain(".facebook.com");
                cookie.setPath("/");
                cookie.setHttpOnly(false);
                cookie.setSecure(!deleted);
                return cookie;
            }

            @Override
            public List<HttpCookie> getCookies() {
                return null;
            }

            @Override
            public List<URI> getURIs() {
                return null;
            }

            @Override
            public boolean remove(URI uri, HttpCookie cookie) {
                return false;
            }

            @Override
            public boolean removeAll() {
                return false;
            }
        }, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
*/
        return webView;
    }

    @Override
    public boolean isWebViewInIFrame() {
        return false;
    }
}
