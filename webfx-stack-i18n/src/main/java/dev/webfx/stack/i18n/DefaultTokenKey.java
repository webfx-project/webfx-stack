package dev.webfx.stack.i18n;

import javafx.scene.Node;

public enum DefaultTokenKey implements TokenKey {
    TEXT(String.class),
    PROMPT(String.class),
    GRAPHIC(Node.class);

    private final Class<?> expectedClass;

    DefaultTokenKey(Class<?> expectedClass) {
        this.expectedClass = expectedClass;
    }

    @Override
    public Class<?> expectedClass() {
        return expectedClass;
    }
}
