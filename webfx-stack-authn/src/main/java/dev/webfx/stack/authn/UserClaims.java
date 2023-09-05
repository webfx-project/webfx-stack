package dev.webfx.stack.authn;

import dev.webfx.platform.ast.ReadOnlyAstObject;

/**
 * @author Bruno Salmon
 */
public class UserClaims {

    private final String username;
    private final String email;
    private final String phone;

    private final ReadOnlyAstObject otherClaims;


    public UserClaims(String username, String email, String phone, ReadOnlyAstObject otherClaims) {
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

    public ReadOnlyAstObject getOtherClaims() {
        return otherClaims;
    }
}
