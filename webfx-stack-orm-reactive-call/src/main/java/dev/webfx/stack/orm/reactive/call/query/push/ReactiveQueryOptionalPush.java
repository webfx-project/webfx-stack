package dev.webfx.stack.orm.reactive.call.query.push;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import dev.webfx.stack.orm.reactive.call.query.ReactiveQueryCall;
import dev.webfx.stack.orm.reactive.call.SwitchableReactiveCall;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.db.query.QueryResult;

/**
 * @author Bruno Salmon
 */
public final class ReactiveQueryOptionalPush extends SwitchableReactiveCall<QueryArgument, QueryResult> {

    private final ReactiveQueryCall reactiveQueryCall;
    private final ReactiveQueryPushCall reactiveQueryPush;

    public ReactiveQueryOptionalPush(ReactiveQueryCall reactiveQueryCall, ReactiveQueryPushCall reactiveQueryPush) {
        this.reactiveQueryCall = reactiveQueryCall;
        this.reactiveQueryPush = reactiveQueryPush;
    }

    public ReactiveQueryCall getReactiveQueryCall() {
        return reactiveQueryCall;
    }

    public ReactiveQueryPushCall getReactiveQueryPush() {
        return reactiveQueryPush;
    }

    private final BooleanProperty pushProperty = new SimpleBooleanProperty(false) {
        @Override
        protected void invalidated() {
            updateDelegate();
        }
    };

    public BooleanProperty pushProperty() {
        return pushProperty;
    }

    public boolean isPush() {
        return pushProperty.get();
    }

    public void setPush(boolean push) {
        pushProperty.set(push);
    }

    private void updateDelegate() {
        setDelegate(isPush() ? reactiveQueryPush : reactiveQueryCall);
    }

    @Override
    protected void onStarted() {
        updateDelegate();
        super.onStarted();
    }
}
