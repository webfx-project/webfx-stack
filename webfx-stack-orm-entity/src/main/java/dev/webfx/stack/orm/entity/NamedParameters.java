package dev.webfx.stack.orm.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class NamedParameters {

    private final List<NamedParameter> namedParameters = new ArrayList<>();

    public NamedParameters add(String name, Object value) {
        namedParameters.add(new NamedParameter(name, value));
        return this;
    }

    public NamedParameter[] get() {
        return namedParameters.toArray(new NamedParameter[0]);
    }
}
