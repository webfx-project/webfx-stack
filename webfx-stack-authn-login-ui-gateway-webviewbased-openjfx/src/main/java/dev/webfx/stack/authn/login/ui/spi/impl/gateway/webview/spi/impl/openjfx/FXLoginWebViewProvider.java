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

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

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
        // popups by default, unless we set a handler that is responsible for their creation. So that's what we do here:
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
            if (popupDialogCallback != null) { // indicates that there was login popup dialog
                popupDialogCallback.closeDialog(); // we close that dialog, because this state change must indicate the success callback
                popupDialogCallback = null; // No need to do it again
            }

            // Also we remove the ugly DOM element from the Facebook login that says the browser is not supported
/* Uncomment this to see the html code of the page rendered in the web view
            String html = (String) mainWebEngine.executeScript("document.documentElement.outerHTML");
            System.out.println(html);
*/
            // The ugly Facebook DOM element id is 'header-notices', so we remove it if we find it.
            Document document = mainWebEngine.getDocument();
            Element headerNotices = document == null ? null : document.getElementById("header-notices");
            if (headerNotices != null)
                headerNotices.getParentNode().removeChild(headerNotices);
        });

        // Setting a cookie handler with a persistent cookie store. This is mainly for the Facebook login which prompts
        // an annoying cookie window. Thanks to the cookie persistence, this should now happen only once, on first time.
        CookieHandler.setDefault(new CookieManager(new FXLoginCookieStore(), CookiePolicy.ACCEPT_ALL));

        return webView;
    }

    @Override
    public boolean isWebViewInIFrame() {
        return false;
    }
}
