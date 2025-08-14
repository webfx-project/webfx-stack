package dev.webfx.stack.orm.dql.sqlcompiler.terms;

import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.dql.sqlcompiler.ExpressionSqlCompiler;
import dev.webfx.stack.orm.expression.terms.Select;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public abstract class AbstractTermSqlCompiler<E extends Expression> {

    private final Class<? extends Expression>[] supportedTermClasses;

    @SafeVarargs
    public AbstractTermSqlCompiler(Class<? extends Expression>... supportedTermClasses) {
        this.supportedTermClasses = supportedTermClasses;
    }

    public Class<? extends Expression>[] getSupportedTermClasses() {
        return supportedTermClasses;
    }

    public abstract void compileExpressionToSql(E e, Options o);

    protected void compileChildExpressionToSql(Expression<?> e, Options o) {
        ExpressionSqlCompiler.compileExpression(e, o);
    }

    protected void compileExpressionPersistentTermsToSql(Expression e, Options o) {
        List<Expression> persistentTerms = new ArrayList<>();
        e.collectPersistentTerms(persistentTerms);
        for (Expression<?> term : persistentTerms)
            compileChildExpressionToSql(term, o);
    }

    protected void compileSelect(Select<?> select, Options o) {
        ExpressionSqlCompiler.buildSelect(select, o);
    }

}
