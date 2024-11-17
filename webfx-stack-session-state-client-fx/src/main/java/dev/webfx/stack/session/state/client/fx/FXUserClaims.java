package dev.webfx.stack.session.state.client.fx;

import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.stack.authn.AuthenticationService;
import dev.webfx.stack.authn.UserClaims;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Bruno Salmon
 */
public final class FXUserClaims {

    private final static ObjectProperty<UserClaims> userClaimsProperty = new SimpleObjectProperty<>();

    public static UserClaims getUserClaims() {
        return userClaimsProperty.get();
    }

    public static ObjectProperty<UserClaims> userClaimsProperty() {
        return userClaimsProperty;
    }

    private static void setUserClaims(UserClaims userClaims) {
        FXProperties.setIfNotEquals(userClaimsProperty, userClaims);
    }

    static { // All FXClass in this package should call FXInit.init() in their static initializer
        FXInit.init(); // See FXInit comments to understand why
    }

    static void init() {
        FXProperties.runNowAndOnPropertyChange(() -> {
            // Forgetting the previous user claims on user change
            setUserClaims(null);
            // Asking the new user claim if the user is logged in (ignored if the user just logged out)
            if (FXLoggedIn.isLoggedIn()) {
                AuthenticationService.getUserClaims()
                        .onFailure(Console::log)
                        .onSuccess(userClaims -> UiScheduler.runInUiThread(() -> setUserClaims(userClaims)));
            }
        }, FXUserId.userIdProperty());
    }

}
