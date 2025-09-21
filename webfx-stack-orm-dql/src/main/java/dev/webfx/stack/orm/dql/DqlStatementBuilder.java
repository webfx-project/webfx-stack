package dev.webfx.stack.orm.dql;

import dev.webfx.platform.ast.ReadOnlyAstObject;
import dev.webfx.platform.ast.json.Json;
import dev.webfx.platform.util.Strings;

import java.util.concurrent.atomic.AtomicInteger;

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
        mergeColumns(f.getColumns());
        AtomicInteger clause1CumulativeParamShift = new AtomicInteger();
        AtomicInteger clause2CumulativeParamShift = new AtomicInteger();
        mergeWhere(f.getWhere(),     clause1CumulativeParamShift, clause2CumulativeParamShift);
        mergeGroupBy(f.getGroupBy(), clause1CumulativeParamShift, clause2CumulativeParamShift);
        mergeHaving(f.getHaving(),   clause1CumulativeParamShift, clause2CumulativeParamShift);
        mergeOrderBy(f.getOrderBy(), clause1CumulativeParamShift, clause2CumulativeParamShift);
        mergeLimit(f.getLimit(),     clause1CumulativeParamShift, clause2CumulativeParamShift);
    }

    /* Fluent API mergers */

    private void mergeAlias(String alias) {
        if (alias != null)
            setAlias(alias);
    }

    public DqlStatementBuilder mergeFields(String fields) {
        return setFields(mergeFields(this.fields, fields));
    }

    private void mergeColumns(String columns) {
        setColumns(mergeColumns(this.columns, columns));
    }

    private void mergeWhere(DqlClause where, AtomicInteger clause1CumulativeParamShift, AtomicInteger clause2CumulativeParamShift) {
        if (where2DoesntImpactWhere1(this.where, where)) { // ex: this.where = "false"
            // We ignore `where` (and its parameters) so subsequent clause2 parameters indexes must be decreased
            clause2CumulativeParamShift.getAndAdd(getClauseParamCount(this.where) - getClauseParamCount(where));
        } else if (where2DoesntImpactWhere1(where, this.where)) { // ex: this.where = "true"
            // We ignore `this.where` (and its parameters) so subsequent clause 1 parameters indexes must be decreased
            clause1CumulativeParamShift.getAndAdd(-getClauseParamCount(this.where)); // probably 0 but just in case
            // and replace it with the provided `where`
            setWhere(mergeDqlClauses(null, where, clause1CumulativeParamShift, clause2CumulativeParamShift));
        } else {
            setWhere(mergeDqlClauses(this.where, where, clause1CumulativeParamShift, clause2CumulativeParamShift, " and ", true));
        }
    }

    private void mergeGroupBy(DqlClause groupBy, AtomicInteger clause1CumulativeParamShift, AtomicInteger clause2CumulativeParamShift) {
        setGroupBy(mergeDqlClauses(this.groupBy, groupBy, clause1CumulativeParamShift, clause2CumulativeParamShift));
    }

    private void mergeHaving(DqlClause having, AtomicInteger clause1CumulativeParamShift, AtomicInteger clause2CumulativeParamShift) {
        setHaving(mergeDqlClauses(this.having, having, clause1CumulativeParamShift, clause2CumulativeParamShift));
    }

    private void mergeOrderBy(DqlClause orderBy, AtomicInteger clause1CumulativeParamShift, AtomicInteger clause2CumulativeParamShift) {
        setOrderBy(mergeDqlClauses(this.orderBy, orderBy, clause1CumulativeParamShift, clause2CumulativeParamShift));
    }

    private void mergeLimit(DqlClause limit, AtomicInteger clause1CumulativeParamShift, AtomicInteger clause2CumulativeParamShift) {
        // for limit, we erase the previous value
        boolean acceptLimit = limit != null;
        int clauseShift = acceptLimit ? clause2CumulativeParamShift.get() : clause1CumulativeParamShift.get();
        if (!acceptLimit)
            limit = this.limit;
        if (limit != null) {
            String dql = limit.getDql();
            int paramCount = getClauseParamCount(limit);
            if (paramCount > 0 && (clauseShift > 0 || dql.contains("?"))) {
                dql = shiftParameterIndexes(dql, clauseShift);
                limit = DqlClause.create(dql, limit.getParameterValues());
            }
            setLimit(limit);
        }
    }

    // private static merge methods

    private static boolean where2DoesntImpactWhere1(DqlClause where1, DqlClause where2) {
        return where2 == null || DqlClause.isClauseFalse(where1) || DqlClause.isClauseTrue(where2);
    }

    private static int getClauseParamCount(DqlClause clause) {
        return clause == null ? 0 : clause.getParameterValues().length;
    }

    public static String mergeFields(String fields1, String fields2) {
        return Strings.isEmpty(fields1) ? fields2 : Strings.isEmpty(fields2) ? fields1 : fields1 + ',' + fields2;
    }

    private static DqlClause mergeDqlClauses(DqlClause clause1, DqlClause clause2, AtomicInteger clause1CumulativeParamShift, AtomicInteger clause2CumulativeParamShift, String separator, boolean parenthesis) {
        String dql1 = clause1 == null ? null : clause1.getDql();
        String dql2 = clause2 == null ? null : clause2.getDql();
        int param1Count = getClauseParamCount(clause1);
        int param2Count = getClauseParamCount(clause2);
        int clause1Shift = clause1CumulativeParamShift.getAndAdd(param2Count);
        if (param1Count > 0 && (clause1Shift > 0 || dql1.contains("?"))) {
            dql1 = shiftParameterIndexes(dql1, clause1Shift);
        }
        // If dql2 has parameters, we shift their indexes by param1Count. We do that always if param1Count > 0, but also
        // if param1Count = 0 in the case where dql2 contains "?" parameters, the reason being that we prefer to convert
        // them to positional parameters "$1", "$2", ... because they are more reliable when used in inline functions.
        // Ex: if fn(p) = p + p, then fn($1) = $1 + $1 works, but not fn(?) = ? + ? (consumes 2 parameters instead of 1).
        int clause2Shift = clause2CumulativeParamShift.addAndGet(param1Count);
        if (param2Count > 0 && (clause2Shift > 0 || dql2.contains("?"))) {
            dql2 = shiftParameterIndexes(dql2, clause2Shift);
        }
        if (Strings.isEmpty(dql1))
            return Strings.isEmpty(dql2) || dql2 == clause2.getDql() ? clause2 : DqlClause.create(dql2, clause2.getParameterValues());
        if (Strings.isEmpty(dql2))
            return dql1 == clause1.getDql() ? clause1 : DqlClause.create(dql1, clause1.getParameterValues());
        if (parenthesis) {
            dql1 = "(" + dql1 + ")";
            dql2 = "(" + dql2 + ")";
        }
        String dql = dql1 + separator + dql2;
        return DqlClause.create(dql, DqlClause.concatClauseParameterValues(clause1, clause2));
    }

    private static DqlClause mergeDqlClauses(DqlClause clause1, DqlClause clause2, AtomicInteger clause1CumulativeParamShift, AtomicInteger clause2CumulativeParamShift) {
        return mergeDqlClauses(clause1, clause2, clause1CumulativeParamShift, clause2CumulativeParamShift, ", ", false);
    }

    private static String mergeColumns(String columns1, String columns2) {
        return Strings.isEmpty(columns1) ? columns2 : Strings.isEmpty(columns2) ? columns1 : Strings.removeSuffix(columns1, "]") + ',' + Strings.removePrefix(columns2, "[");
    }

    static String shiftParameterIndexes(String dql, int shift) {
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
