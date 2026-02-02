package dev.webfx.stack.routing.uirouter;

import dev.webfx.platform.async.Handler;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.windowhistory.spi.BrowsingHistory;
import dev.webfx.platform.windowhistory.spi.BrowsingHistoryLocation;
import dev.webfx.stack.routing.router.Router;
import dev.webfx.stack.session.state.client.fx.FXAuthorizationsWaiting;

/**
 * @author Bruno Salmon
 */
public class HistoryRouter {

    protected final Router router;
    protected BrowsingHistory history;
    // The default path to be used if the history is initially empty or the path is not found
    private String defaultInitialHistoryPath;

    public HistoryRouter(Router router, BrowsingHistory history) {
        this.router = router;
        this.history = history;
        router.exceptionHandler(new Handler<>() {
            @Override
            public void handle(Throwable throwable) {
                Console.error("Path not found", throwable);
                router.exceptionHandler(null); // removing the handler to avoid an infinite recursion if the default path can't be found
                replaceCurrentHistoryWithInitialDefaultPath();
                router.exceptionHandler(this); // restoring the handler
            }
        });
        // Refreshing the page each time the authorizations change
        FXAuthorizationsWaiting.runOnAuthorizationsChangedOrWaiting(this::refresh);
    }

    public Router getRouter() {
        return router;
    }

    public BrowsingHistory getHistory() {
        return history;
    }

    protected void setHistory(BrowsingHistory history) {
        this.history = history;
    }

    public String getDefaultInitialHistoryPath() {
        return defaultInitialHistoryPath;
    }

    public void setDefaultInitialHistoryPath(String defaultInitialHistoryPath) {
        this.defaultInitialHistoryPath = defaultInitialHistoryPath;
    }

    protected void replaceCurrentHistoryWithInitialDefaultPath() {
        if (defaultInitialHistoryPath != null)
            history.replace(defaultInitialHistoryPath);
    }

    public void start() {
        history.listen(this::onNewHistoryLocation);
        refresh();
    }

    public void refresh() {
        onNewHistoryLocation(history.getCurrentLocation());
    }

    protected void onNewHistoryLocation(BrowsingHistoryLocation browsingHistoryLocation) {
        String path;
        Object state;
        // On the first call, browsingHistoryLocation might be null when not running in the browser
        if (browsingHistoryLocation == null) { // in-memory history not yet initialized
            path = defaultInitialHistoryPath;
            state = null;
            history.push(path); // initialising in-memory history to the default initial path
        } else { // general case (browser history or in-memory history but initialized)
            path = history.getPath(browsingHistoryLocation);
            state = browsingHistoryLocation.getState();
        }
        // Also on first call, the path might be empty in the browser if the location is just the domain name
        if (path == null || path.isEmpty()) { // In that case, we route to the initial default path
            path = defaultInitialHistoryPath;
        }
        // Submitting the new path & state to the router, and this even if the path & state didn't change, as the router
        // may behave differently in dependence on other factors (ex: it may show a login window on first attempt, then
        // the actual requested page on second attend (if logged in and authorized) or the unauthorized page (if logged
        // in but not authorized).
        router.accept(path, state);
    }

}
