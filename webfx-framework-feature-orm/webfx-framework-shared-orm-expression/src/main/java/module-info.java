// File managed by WebFX (DO NOT EDIT MANUALLY)

module webfx.framework.shared.orm.expression {

    // Direct dependencies modules
    requires java.base;
    requires webfx.extras.type;
    requires webfx.lib.javacupruntime;
    requires webfx.platform.shared.json;
    requires webfx.platform.shared.log;
    requires webfx.platform.shared.util;

    // Exported packages
    exports dev.webfx.stack.framework.shared.orm.expression;
    exports dev.webfx.stack.framework.shared.orm.expression.builder;
    exports dev.webfx.stack.framework.shared.orm.expression.builder.terms;
    exports dev.webfx.stack.framework.shared.orm.expression.lci;
    exports dev.webfx.stack.framework.shared.orm.expression.parser;
    exports dev.webfx.stack.framework.shared.orm.expression.parser.javacup;
    exports dev.webfx.stack.framework.shared.orm.expression.parser.jflex;
    exports dev.webfx.stack.framework.shared.orm.expression.parser.lci;
    exports dev.webfx.stack.framework.shared.orm.expression.parser.lci.mock;
    exports dev.webfx.stack.framework.shared.orm.expression.terms;
    exports dev.webfx.stack.framework.shared.orm.expression.terms.function;
    exports dev.webfx.stack.framework.shared.orm.expression.terms.function.java;

}