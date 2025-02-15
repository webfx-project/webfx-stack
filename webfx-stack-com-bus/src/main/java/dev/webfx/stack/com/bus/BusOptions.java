package dev.webfx.stack.com.bus;


import dev.webfx.platform.util.Objects;
import dev.webfx.platform.ast.ReadOnlyAstObject;

/**
 * @author Bruno Salmon
 */
public class BusOptions {

    private String busPrefix;

    public BusOptions turnUnsetPropertiesToDefault() {
        busPrefix = Objects.coalesce(busPrefix, "eventbus");
        return this;
    }

    public BusOptions applyConfig(ReadOnlyAstObject config) {
        busPrefix = config.getString("busPrefix", busPrefix);
        return this;
    }

    public String getBusPrefix() {
        return busPrefix;
    }

}
