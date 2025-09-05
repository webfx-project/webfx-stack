package dev.webfx.stack.orm.dql.sqlcompiler.sql.dbms;

/**
 * @author Bruno Salmon
 */
public class DbmsSqlSyntaxImpl implements DbmsSqlSyntax {

    private final boolean repeatTableAliasAfterDelete;
    private final boolean hasInsertReturningClause;

    public DbmsSqlSyntaxImpl(boolean repeatTableAliasAfterDelete, boolean hasInsertReturningClause) {
        this.repeatTableAliasAfterDelete = repeatTableAliasAfterDelete;
        this.hasInsertReturningClause = hasInsertReturningClause;
    }

    public boolean repeatTableAliasAfterDelete() {
        return repeatTableAliasAfterDelete;
    }

    public boolean hasInsertReturningClause() {
        return hasInsertReturningClause;
    }

    public boolean isReservedIdentifier(String identifier) {
        return switch (identifier.toLowerCase()) {
            case "abs", "absolute", "action", "add", "all", "allocate", "alter", "and", "any", "are", "array",
                 "array_agg", "as", "asc", "asensitive", "assertion", "asymmetric", "at", "atomic", "authorization",
                 "avg", "begin", "begin_frame", "begin_partition", "between", "bigint", "binary", "bit", "bit_length",
                 "blob", "boolean", "both", "by", "call", "called", "cardinality", "cascade", "cascaded", "case",
                 "cast", "catalog", "ceil", "ceiling", "char", "character", "character_length", "char_length", "check",
                 "clob", "close", "coalesce", "collate", "collation", "collect", "column", "commit", "condition",
                 "connect", "connection", "constraint", "constraints", "contains", "continue", "convert", "corr",
                 "corresponding", "count", "covar_pop", "covar_samp", "create", "cross", "cube", "cume_dist", "current",
                 "current_catalog", "current_date", "current_path", "current_role", "current_row", "current_schema",
                 "current_time", "current_timestamp", "current_user", "cursor", "cycle", "datalink", "date", "day",
                 "deallocate", "dec", "decimal", "declare", "default", "deferrable", "deferred", "delete", "dense_rank",
                 "deref", "desc", "describe", "descriptor", "deterministic", "diagnostics", "disconnect", "distinct",
                 "dlnewcopy", "dlpreviouscopy", "dlurlcomplete", "dlurlcompleteonly", "dlurlcompletewrite", "dlurlpath",
                 "dlurlpathonly", "dlurlpathwrite", "dlurlscheme", "dlurlserver", "dlvalue", "domain", "double", "drop",
                 "dynamic", "each", "element", "else", "end", "end-exec", "end_frame", "end_partition", "equals",
                 "escape", "every", "except", "exception", "excec", "excecute", "exists", "exp", "external", "extract",
                 "false", "fetch", "filter", "first", "first_value", "float", "floor", "for", "foreign", "found",
                 "frame_raw", "from", "full", "function", "fusion", "get", "global", "go", "goto", "grant", "group",
                 "grouping", "groups", "having", "hold", "hour", "identity", "immediate", "import", "in", "indicator",
                 "initially", "inner", "inout", "input", "insensitive", "insert", "int", "integer", "intersect",
                 "intersection", "interval", "into", "is", "isolation", "join", "key", "lag", "language", "large",
                 "last", "last_value", "lateral", "lead", "leading", "left", "level", "like", "like_regex", "ln",
                 "local", "localtime", "localtimestamp", "lower", "match", "max", "max_cardinality", "member", "merge",
                 "method", "min", "minute", "mod", "modifies", "module", "month", "multiset", "names", "national",
                 "natural", "nchar", "nclob", "new", "next", "no", "none", "normalize", "not", "nth_value", "ntile",
                 "null", "nullif", "numeric", "occurrences_regex", "octet_length", "of", "offset", "old", "on", "only",
                 "open", "option", "or", "order", "out", "outer", "output", "over", "overlaps", "overlay", "parameter",
                 "partial", "partition", "percent", "percentile_cont", "percentile_disc", "percent_rank", "period",
                 "portion", "position", "position_regex", "power", "precedes", "precision", "prepare", "preserve",
                 "primary", "privileges", "procedure", "public", "range", "rank", "read", "reads", "real", "recursive",
                 "ref", "references", "referencing", "regr_avgx", "regr_avgy", "regr_count", "regr_intercept",
                 "regr_r2", "regr_slope", "regr_sxx", "regr_sxy", "regr_syy", "relative", "release", "restrict",
                 "result", "return", "returns", "revoke", "right", "rollback", "rollup", "row", "rows", "row_number",
                 "save_point", "schema", "scope", "scroll", "search", "second", "section", "select", "sensitive",
                 "session", "session_user", "set", "similar", "size", "smallint", "some", "space", "specific",
                 "specifictype", "sql", "sqlcode", "sqlerror", "sqlexception", "sqlstate", "sqlwarning", "sqrt",
                 "start", "static", "stddev_pop", "stddev_samp", "submultiset", "substring", "substring_regex",
                 "succeeds", "sum", "symetric", "system", "system_time", "system_user", "table", "tablesample",
                 "temporary", "then", "time", "timestamp", "timezone_hour", "timezone_minute", "to", "trailing",
                 "transaction", "translate", "translate_regex", "translation", "treat", "trigger", "trim", "trim_array",
                 "true", "truncate", "uescape", "union", "unique", "unknown", "unnest", "updated", "upper", "usage",
                 "user", "using", "value", "values", "value_of", "varbinary", "varchar", "varying", "var_pop",
                 "var_samp", "versioning", "view", "when", "whenever", "where", "width_bucket", "window", "with",
                 "within", "without", "work", "write", "xml", "xmlagg", "xmlattributes", "xmlbinary", "xmlcast",
                 "xmlcomment", "xmlconcat", "xmldocument", "xmlelement", "xmlexists", "xmlforest", "xmliterate",
                 "xmlnamespaces", "xmlparse", "xmlpi", "xmlquery", "xmlserialize", "xmltable", "xmlvalidate", "year",
                 "zone" -> true;
            default -> false;
        };
    }

}
