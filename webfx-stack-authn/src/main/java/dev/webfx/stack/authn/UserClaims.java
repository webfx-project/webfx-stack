package dev.webfx.stack.authn;

import dev.webfx.platform.ast.ReadOnlyAstObject;

/**
 * @author Bruno Salmon
 */
public record UserClaims(String username, String email, String phone, ReadOnlyAstObject otherClaims) {

}
