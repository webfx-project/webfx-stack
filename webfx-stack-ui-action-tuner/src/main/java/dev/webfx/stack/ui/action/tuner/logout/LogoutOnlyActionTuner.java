package dev.webfx.stack.ui.action.tuner.logout;

import dev.webfx.stack.session.state.client.fx.FXLoggedOut;
import dev.webfx.stack.ui.action.Action;
import dev.webfx.stack.ui.action.ActionBuilder;
import dev.webfx.stack.ui.action.tuner.ActionTuner;
import javafx.beans.property.ReadOnlyBooleanProperty;

/**
 * @author Bruno Salmon
 */
public interface LogoutOnlyActionTuner extends ActionTuner {

    @Override
    default Action tuneAction(Action action) {
        ReadOnlyBooleanProperty loggedOutProperty = FXLoggedOut.loggedOutProperty();
        return new ActionBuilder(action)
            // Important to bind to set disabledProperty and not directly visibleProperty, especially for toggle buttons
            // keyboard navigation (ToggleButtonBehavior skips disabled buttons but not invisible ones)
            .setDisabledProperty(loggedOutProperty.not())
            .setHiddenWhenDisabled(true) // TODO: see if we can get this information from the underlying action instead of forcing it
            .build();
    }

}
