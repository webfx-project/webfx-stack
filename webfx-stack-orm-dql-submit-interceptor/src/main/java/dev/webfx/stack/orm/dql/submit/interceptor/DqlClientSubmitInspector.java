package dev.webfx.stack.orm.dql.submit.interceptor;

import dev.webfx.stack.db.submit.ClientSubmitGuard;
import dev.webfx.stack.db.submit.ClientWriteDenyList;
import dev.webfx.stack.db.submit.ProtectedEntityWriteRegistry;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.orm.datasourcemodel.service.DataSourceModelService;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.Delete;
import dev.webfx.stack.orm.expression.terms.DqlStatement;
import dev.webfx.stack.orm.expression.terms.Equals;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import dev.webfx.stack.orm.expression.terms.Insert;
import dev.webfx.stack.orm.expression.terms.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads a client's DQL write for {@link ClientSubmitGuard}: refuses it when it sets a column in
 * {@link ClientWriteDenyList}, and otherwise reports the write it makes, for the application's row rules.
 *
 * <p>Judged on the domain model's resolution of every assignment — the SQL table and column each field maps to —
 * so a write cannot rephrase its way round the list with an alias or a differently spelled name. And judged
 * FAIL-CLOSED wherever the statement cannot be read field by field: an assignment whose left-hand side is not a
 * plain field, or an insert in a form that has no set clause, is refused on a table that has any denied column —
 * and on every table once the application has row rules, since those judge the fields a write names and a field
 * this could not read would pass them unseen. No client sends such a statement: the change-set layer writes plain
 * {@code field = value} assignments. A statement that will not parse at all is refused too; it would not have run.
 *
 * <p>Parses without the cache, like the read inspector: client writes are driven by user actions, in the tens per
 * second at most, and a parse the translation step repeats is cheaper than a second place for a verdict to go
 * stale.
 *
 * @author Claude Code
 */
final class DqlClientSubmitInspector implements ClientSubmitGuard.Inspector {

    @Override
    public ClientSubmitGuard.Inspection inspect(SubmitArgument argument) {
        if (ClientWriteDenyList.isEmpty() && !ClientSubmitGuard.hasWritePolicy())
            return ClientSubmitGuard.Inspection.allowed(null);
        DataSourceModel dataSourceModel = DataSourceModelService.getDataSourceModel(argument.getDataSourceId());
        if (dataSourceModel == null)
            return ClientSubmitGuard.Inspection.refused(ClientSubmitGuard.UNCHECKABLE_REFUSED);
        return inspect(dataSourceModel, argument.getStatement(), argument.getParameters());
    }

    /**
     * The reading itself, given the model to read the statement against. Package-private so the check in
     * scripts/checks/client-write-guard can drive it with the real domain model and no running server.
     */
    static ClientSubmitGuard.Inspection inspect(DataSourceModel dataSourceModel, String dqlStatement, Object[] parameters) {
        DqlStatement<Object> statement;
        try {
            statement = dataSourceModel.parseStatement(dqlStatement);
        } catch (RuntimeException e) {
            return ClientSubmitGuard.Inspection.refused(ClientSubmitGuard.UNCHECKABLE_REFUSED);
        }
        ProtectedEntityWriteRegistry.WriteVerb verb =
              statement instanceof Insert ? ProtectedEntityWriteRegistry.WriteVerb.INSERT
            : statement instanceof Update ? ProtectedEntityWriteRegistry.WriteVerb.UPDATE
            : statement instanceof Delete ? ProtectedEntityWriteRegistry.WriteVerb.DELETE
            : null;
        if (verb == null)
            // Not a write — a SELECT, or another read (WithSelect, Union). The submit endpoint runs it
            // against the database verbatim, and the READ deny list is enforced only on the query path
            // (ClientReadDenyList / ClientQueryGuard), never here. So a read let through here is a second,
            // unguarded door to every column the query path hides: a client could `select token from
            // MagicLink where token like $1` and read the presence of a match back through returnGeneratedKeys.
            // The submit endpoint has no legitimate read — the query endpoint exists for that — so a read
            // arriving here is refused outright rather than run. (Reached only for language=DQL: the guard
            // routes raw statements and transaction preambles elsewhere before the inspector is asked.)
            return ClientSubmitGuard.Inspection.refused(ClientSubmitGuard.NOT_A_WRITE_REFUSED);
        DomainClass domainClass = statement.getDomainClass() instanceof DomainClass dc ? dc
            : dataSourceModel.getDomainModel().getClass(statement.getDomainClass());
        if (domainClass == null)
            return ClientSubmitGuard.Inspection.refused(ClientSubmitGuard.UNCHECKABLE_REFUSED);
        // A table that is nothing but credentials is closed to every write, deletes included
        if (ClientWriteDenyList.isTableDenied(domainClass.getSqlTableName()))
            return ClientSubmitGuard.Inspection.refused(ClientSubmitGuard.DENIED_REFUSED);
        if (verb != ProtectedEntityWriteRegistry.WriteVerb.DELETE) {
            boolean mustReadEveryField = ClientWriteDenyList.isTableGuarded(domainClass.getSqlTableName())
                                         || ClientSubmitGuard.hasWritePolicy();
            ExpressionArray<Object> setClause = statement instanceof Update update ? update.getSetClause()
                : ((Insert<Object>) statement).getSetClause();
            if (setClause == null) {
                if (mustReadEveryField)
                    return ClientSubmitGuard.Inspection.refused(ClientSubmitGuard.UNCHECKABLE_REFUSED);
            } else {
                for (Expression<?> expression : setClause.getExpressions()) {
                    if (expression instanceof Equals<?> equals && equals.getLeft() instanceof DomainField field) {
                        DomainClass fieldClass = field.getDomainClass() != null ? field.getDomainClass() : domainClass;
                        if (ClientWriteDenyList.isColumnDenied(fieldClass.getSqlTableName(), field.getSqlColumnName()))
                            return ClientSubmitGuard.Inspection.refused(ClientSubmitGuard.DENIED_REFUSED);
                    } else if (mustReadEveryField) {
                        return ClientSubmitGuard.Inspection.refused(ClientSubmitGuard.UNCHECKABLE_REFUSED);
                    }
                }
            }
        }
        // The same reading of fields, values and target the application's write authorizer is given - once per
        // ROW, because one argument can carry several (see parameterRowsOf).
        List<Object[]> rows = DqlSubmitInterceptorInitializer.parameterRowsOf(parameters);
        List<ProtectedEntityWriteRegistry.WriteRequest> writes = new ArrayList<>(rows.size());
        // Row-invariant, so read once rather than once per row: both are properties of the statement.
        String entityName = domainClass.getName();
        boolean unbounded = DqlSubmitInterceptorInitializer.isUnboundedWrite(verb, statement);
        for (Object[] row : rows) {
            Map<String, Object> writtenValues = DqlSubmitInterceptorInitializer.writtenValuesOf(statement, row);
            writes.add(new ProtectedEntityWriteRegistry.WriteRequest(
                entityName, verb,
                writtenValues.keySet().toArray(new String[0]),
                writtenValues,
                DqlSubmitInterceptorInitializer.targetIdOf(statement, row),
                unbounded,
                null)); // the batch, if there is one, is attached by ClientSubmitGuard.checkBatch
        }
        return ClientSubmitGuard.Inspection.allowedAll(writes);
    }

}
