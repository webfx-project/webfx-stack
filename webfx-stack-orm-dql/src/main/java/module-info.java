// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.dql {

    // Direct dependencies modules
    requires webfx.extras.type;
    requires webfx.platform.ast;
    requires webfx.platform.ast.json.plugin;
    requires transitive webfx.platform.util;
    requires webfx.platform.util.time;
    requires webfx.stack.orm.expression;

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