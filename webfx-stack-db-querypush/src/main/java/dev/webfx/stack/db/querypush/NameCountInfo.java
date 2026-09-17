package dev.webfx.stack.db.querypush;

/**
 * One bucket of a simple {name → count} distribution shown on the /monitor page — used for the
 * connected-clients breakdown by build version and by PWA display mode. {@code name} is the bucket
 * label (a version string, or {@code "installed"} / {@code "browser"} / {@code "unknown"}).
 *
 * @author Bruno Salmon
 */
public final class NameCountInfo {

    private final String name;
    private final int count;

    public NameCountInfo(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }
}
