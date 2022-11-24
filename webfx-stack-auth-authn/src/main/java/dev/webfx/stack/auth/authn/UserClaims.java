package dev.webfx.stack.auth.authn;

import dev.webfx.platform.json.ReadOnlyJsonObject;

/**
 * @author Bruno Salmon
 */
public class UserClaims {

    private final String username;
    private final String email;
    private final String phone;

    private final ReadOnlyJsonObject otherClaims;


    public UserClaims(String username, String email, String phone, ReadOnlyJsonObject otherClaims) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.otherClaims = otherClaims;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public ReadOnlyJsonObject getOtherClaims() {
        return otherClaims;
    }
}
