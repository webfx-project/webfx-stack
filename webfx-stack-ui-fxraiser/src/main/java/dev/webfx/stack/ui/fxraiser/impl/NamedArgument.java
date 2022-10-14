package dev.webfx.stack.ui.fxraiser.impl;

/**
 * @author Bruno Salmon
 */
public class NamedArgument {

    private final String name;
    private final Object argument;

    public NamedArgument(String name, Object argument) {
        this.name = name;
        this.argument = argument;
    }

    public String getName() {
        return name;
    }

    public Object getArgument() {
        return argument;
    }
}
