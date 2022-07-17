// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.dql {

    // Direct dependencies modules
    requires java.base;
    requires webfx.extras.type;
    requires webfx.platform.util;
    requires webfx.stack.orm.expression;
    requires webfx.stack.platform.json;

    // Exported packages
    exports dev.webfx.stack.orm.dql;
    exports dev.webfx.stack.orm.dql.sqlcompiler;
    exports dev.webfx.stack.orm.dql.sqlcompiler.lci;
    exports dev.webfx.stack.orm.dql.sqlcompiler.lci.mock;
    exports dev.webfx.stack.orm.dql.sqlcompiler.mapping;
    exports dev.webfx.stack.orm.dql.sqlcompiler.sql;
    exports dev.webfx.stack.orm.dql.sqlcompiler.sql.dbms;
    exports dev.webfx.stack.orm.dql.sqlcompiler.terms;

}