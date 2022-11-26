package dev.webfx.stack.session.state;

import java.util.Objects;

public interface LogoutUserId {

    String LOGOUT_USER_ID = "LOGOUT_USER_ID";

    static boolean isLogoutUserId(Object userId) {
        return Objects.equals(userId, LOGOUT_USER_ID);
    }

    static boolean isLogoutUserIdOrNull(Object userId) {
        return userId == null || isLogoutUserId(userId);
    }
}
