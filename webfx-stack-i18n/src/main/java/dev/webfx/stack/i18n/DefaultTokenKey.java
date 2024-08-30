package dev.webfx.stack.i18n;

import javafx.scene.Node;
import javafx.scene.paint.Paint;

public enum DefaultTokenKey implements TokenKey {
    TEXT("text", String.class),
    PROMPT("prompt", String.class),
    GRAPHIC("graphic", Node.class),
    FILL("fill", Paint.class),
    TEXT_FILL("textFill", Paint.class);

    private final String token;
    private final Class<?> expectedClass;

    DefaultTokenKey(String token, Class<?> expectedClass) {
        this.token = token;
        this.expectedClass = expectedClass;
    }

    @Override
    public Class<?> expectedClass() {
        return expectedClass;
    }


    @Override
    public String toString() {
        return token;
    }
}
