package dev.webfx.stack.orm.dql.sqlcompiler.sql.dbms;

/**
 * @author Bruno Salmon
 */
public final class PostgresSyntax extends DbmsSqlSyntaxImpl {

    private final static PostgresSyntax INSTANCE = new PostgresSyntax();

    public static PostgresSyntax get() {
        return INSTANCE;
    }

    public PostgresSyntax() {
        super(false, true);
    }

    public boolean isReservedIdentifier(String identifier) {
        return switch (identifier = identifier.toLowerCase()) {
            case "analyse", "analyze", "concurrently", "do", "freeze", "isnull", "limit", "notnull", "placing",
                 "returning", "variadic", "verbose" -> true;
            default -> super.isReservedIdentifier(identifier);
        };
    }
}
