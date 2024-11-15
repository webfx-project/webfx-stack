package dev.webfx.stack.authn.login.ui.spi;

import javafx.beans.property.StringProperty;
import javafx.scene.Node;

import java.util.function.Consumer;

public interface UiLoginServiceProvider {

    Node createLoginUi();

    Node createMagicLinkUi(StringProperty magicLinkTokenProperty, Consumer<String> requestedPathConsumer);

}
