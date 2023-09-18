// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.stack.orm.expression {

    // Direct dependencies modules
    requires java.base;
    requires webfx.extras.type;
    requires webfx.lib.javacupruntime;
    requires webfx.platform.ast;
    requires webfx.platform.console;
    requires webfx.platform.util;

    // Exported packages
    exports dev.webfx.stack.orm.expression;
    exports dev.webfx.stack.orm.expression.builder;
    exports dev.webfx.stack.orm.expression.builder.terms;
    exports dev.webfx.stack.orm.expression.lci;
    exports dev.webfx.stack.orm.expression.parser;
    exports dev.webfx.stack.orm.expression.parser.javacup;
    exports dev.webfx.stack.orm.expression.parser.jflex;
    exports dev.webfx.stack.orm.expression.parser.lci;
    exports dev.webfx.stack.orm.expression.parser.lci.mock;
    exports dev.webfx.stack.orm.expression.terms;
    exports dev.webfx.stack.orm.expression.terms.function;
    exports dev.webfx.stack.orm.expression.terms.function.java;

}