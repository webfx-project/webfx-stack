package dev.webfx.stack.framework.shared.services.authz.mixin;

import javafx.beans.value.ObservableValue;

/**
 * @author Bruno Salmon
 */
public interface HasUserPrincipalProperty extends HasUserPrincipal {

    ObservableValue userPrincipalProperty();

    @Override
    default Object getUserPrincipal() {
        return userPrincipalProperty().getValue();
    }


}
