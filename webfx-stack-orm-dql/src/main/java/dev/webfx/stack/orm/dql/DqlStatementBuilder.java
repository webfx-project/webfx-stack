package dev.webfx.stack.orm.dql;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.util.Arrays;
import dev.webfx.platform.util.Strings;

/**
 * @author Bruno Salmon
 */
public final class DqlStatementBuilder {

    private final Object domainClassId;
    private String alias;
    private String fields;
    private DqlClause where;
    private DqlClause groupBy;
    private DqlClause having;
    private DqlClause orderBy;
    private DqlClause limit;
    // The 'columns' field is for display purpose only, it is not included in the resulting SQL query. So any persistent
    // fields required for the 'columns' evaluation should be loaded by including them in the 'fields' field.
    private String columns;

    public DqlStatementBuilder() {
        domainClassId = null;
    }

    public DqlStatementBuilder(Object jsonOrClass) {
        String s = jsonOrClass instanceof String ? (String) jsonOrClass : null;
        if ((s == null || s.indexOf('{') == -1) && !(jsonOrClass instanceof ReadOnlyAstObject))
            domainClassId = jsonOrClass;
        else {
            ReadOnlyAstObject json = s != null ? Json.parseObject(s) : (ReadOnlyAstObject) jsonOrClass;
            domainClassId = json.get("class");
            applyJson(json);
        }
    }

    public DqlStatementBuilder(ReadOnlyAstObject json) {
        domainClassId = json.get("class");
        applyJson(json);
    }

    public DqlStatementBuilder(DqlStatement dqlStatement) {
        domainClassId = dqlStatement.getDomainClassId();
        applyFilter(dqlStatement);
    }

    public DqlStatement build() {
        return new DqlStatement(domainClassId, alias, fields, where, groupBy, having, orderBy, limit, columns);
    }

    public String getColumns() {
        return columns;
    }

    private boolean isApplicable(DqlStatement dqlStatement) {
        return isApplicable(dqlStatement.getDomainClassId());
    }

    private boolean isApplicable(Object domainClassId) {
        return domainClassId == null || this.domainClassId == null || this.domainClassId.equals(domainClassId);
    }

    public DqlStatementBuilder applyFilter(DqlStatement f) {
        if (f == null)
            return this;
        if (!isApplicable(f))
            throw new IllegalArgumentException();
        setAlias(f.getAlias());
        setFields(f.getFields());
        setWhere(f.getWhere());
        setGroupBy(f.getGroupBy());
        setHaving(f.getHaving());
        setOrderBy(f.getOrderBy());
        setLimit(f.getLimit());
        setColumns(f.getColumns());
        return this;
    }

    public DqlStatementBuilder applyJson(ReadOnlyAstObject json) {
        if (json == null)
            return this;
        if (!isApplicable(json.getString("class")))
            throw new IllegalArgumentException();
        setAlias(json.getString("alias"));
        setFields(DqlStatement.getPossibleArrayAsString(json, "fields"));
        setWhere(DqlClause.create(json.getString("where")));
        setGroupBy(DqlClause.create(json.getString("groupBy")));
        setHaving(DqlClause.create(json.getString("having")));
        setOrderBy(DqlClause.create(json.getString("orderBy")));
        setLimit(DqlClause.create(json.getString("limit")));
        setColumns(DqlStatement.getPossibleArrayAsString(json, "columns"));
        return this;
    }

    /* Fluent API setters */

    public DqlStatementBuilder setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public DqlStatementBuilder setWhere(DqlClause where) {
        this.where = where;
        return this;
    }

    public DqlStatementBuilder setFields(String fields) {
        this.fields = fields;
        return this;
    }

    public DqlStatementBuilder setGroupBy(DqlClause groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    public DqlStatementBuilder setHaving(DqlClause having) {
        this.having = having;
        return this;
    }

    public DqlStatementBuilder setOrderBy(DqlClause orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public DqlStatementBuilder setLimit(DqlClause limit) {
        this.limit = limit;
        return this;
    }

    public DqlStatementBuilder setColumns(String columns) {
        this.columns = columns;
        return this;
    }


    public void merge(DqlStatement f) {
        if (f == null)
            return;
        if (!isApplicable(f))
            throw new IllegalArgumentException("Trying to merge filters with different classes (" + domainClassId + " / " + f.getDomainClassId() + ")");
        mergeAlias(f.getAlias());
        mergeFields(f.getFields());
        mergeWhere(f.getWhere());
        mergeGroupBy(f.getGroupBy());
        mergeHaving(f.getHaving());
        mergeOrderBy(f.getOrderBy());
        mergeLimit(f.getLimit());
        mergeColumns(f.getColumns());
    }

    /* Fluent API mergers */

    public DqlStatementBuilder mergeAlias(String alias) {
        if (alias != null)
            setAlias(alias);
        return this;
    }

    public DqlStatementBuilder mergeFields(String fields) {
        return setFields(mergeFields(this.fields, fields));
    }

    public DqlStatementBuilder mergeWhere(DqlClause where) {
        if (where != null && !DqlClause.isClauseFalse(this.where) && !DqlClause.isClauseTrue(where)) {
            if (this.where == null || DqlClause.isClauseFalse(where) || DqlClause.isClauseTrue(this.where))
                setWhere(where);
            else
                setWhere(mergeDqlClauses(this.where, where, " and ", true));
        }
        return this;
    }

    public DqlStatementBuilder mergeGroupBy(DqlClause groupBy) {
        return setGroupBy(mergeDqlClauses(this.groupBy, groupBy));
    }

    public DqlStatementBuilder mergeHaving(DqlClause having) {
        return setHaving(mergeDqlClauses(this.having, having));
    }

    public DqlStatementBuilder mergeOrderBy(DqlClause orderBy) {
        return setOrderBy(mergeDqlClauses(this.orderBy, orderBy));
    }

    public DqlStatementBuilder mergeLimit(DqlClause limit) {
        // for limit, we erase the previous value
        if (limit != null)
            setLimit(limit);
        return this;
    }

    public DqlStatementBuilder mergeColumns(String columns) {
        setColumns(mergeColumns(this.columns, columns));
        return this;
    }

    // private static merge methods

    public static String mergeFields(String fields1, String fields2) {
        return Strings.isEmpty(fields1) ? fields2 : Strings.isEmpty(fields2) ? fields1 : fields1 + ',' + fields2;
    }

    private static DqlClause mergeDqlClauses(DqlClause clause1, DqlClause clause2, String separator, boolean parenthesis) {
        String dql1 = clause1 == null ? null : clause1.getDql();
        if (Strings.isEmpty(dql1))
            return clause2;
        String dql2 = clause2 == null ? null : clause2.getDql();
        if (Strings.isEmpty(dql2))
            return clause1;
        int param1Count = Arrays.length(clause1.getParameterValues());
        if (param1Count > 0) {
            int param2Count = Arrays.length(clause2.getParameterValues());
            if (param2Count > 0) {
                dql2 = shiftParameterIndexes(dql2, param1Count);
            }
        }
        if (parenthesis) {
            dql1 = "(" + dql1 + ")";
            dql2 = "(" + dql2 + ")";
        }
        String dql = dql1 + separator + dql2;
        return DqlClause.create(dql, DqlClause.concatClauseParameterValues(clause1, clause2));
    }

    private static DqlClause mergeDqlClauses(DqlClause clause1, DqlClause clause2) {
        return mergeDqlClauses(clause1, clause2, ", ", false);
    }

    private static String mergeColumns(String columns1, String columns2) {
        return Strings.isEmpty(columns1) ? columns2 : Strings.isEmpty(columns2) ? columns1 : Strings.removeSuffix(columns1, "]") + ',' + Strings.removePrefix(columns2, "[");
    }

    private static String shiftParameterIndexes(String dql, int shift) {
        // If dql contains indexed parameters $1, $2, ... then we need to increment these indexes by shift
        // Additionally, replace any positional '?' parameters with next indexes starting at shift, then shift+1, ...
        StringBuilder sb = new StringBuilder();
        int nextQIndex = shift + 1; // the first '?' becomes $(shift+1)
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < dql.length(); i++) {
            char c = dql.charAt(i);
            // Handle simple string literal boundaries to avoid rewriting placeholders inside strings
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                sb.append(c);
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                sb.append(c);
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '$') {
                    int j = i + 1;
                    int start = j;
                    while (j < dql.length()) {
                        char dj = dql.charAt(j);
                        if (dj >= '0' && dj <= '9') {
                            j++;
                        } else {
                            break;
                        }
                    }
                    if (j > start) {
                        int idx = Integer.parseInt(dql.substring(start, j));
                        int newIdx = idx + shift;
                        sb.append('$').append(newIdx);
                        i = j - 1; // advance
                        continue;
                    }
                    // '$' not followed by digits
                    sb.append(c);
                    continue;
                }
                if (c == '?') {
                    // Replace '?' with the next sequential index starting at shift
                    sb.append('$').append(nextQIndex);
                    nextQIndex++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

}
