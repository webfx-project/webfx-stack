package dev.webfx.stack.auth.login.ui.spi.impl.webthirdparty;

import dev.webfx.platform.console.Console;
import dev.webfx.stack.auth.login.LoginService;
import dev.webfx.stack.auth.login.ui.spi.LoginUiProvider;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * @author Bruno Salmon
 */
public class WebThirdPartyLoginUiProvider implements LoginUiProvider {

    @Override
    public Node createLoginUi() {
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
/*
        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                Console.log("READY");
                JSObject jsobj = (JSObject) engine.executeScript("window");
                jsobj.setMember("provider", WebThirdPartyLoginUiProvider.this);
                engine.executeScript("mojoTest()");
            }
        });
*/
        LoginService.getLoginUiInput()
                .onSuccess(html -> Platform.runLater(() -> engine.loadContent((String) html)));
        return webView;
    }

    public void log(Object response) {
        Console.log("JSCallback: " + response);
    }
}
