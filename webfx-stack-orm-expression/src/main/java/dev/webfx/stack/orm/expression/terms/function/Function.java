package dev.webfx.stack.orm.expression.terms.function;

import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.type.SpecializedTextType;
import dev.webfx.extras.type.Type;
import dev.webfx.stack.orm.expression.lci.DomainReader;
import dev.webfx.stack.orm.expression.terms.function.java.Coalesce;
import dev.webfx.stack.orm.expression.terms.function.java.CurrentDate;
import dev.webfx.stack.orm.expression.terms.function.java.StringAgg;
import dev.webfx.stack.orm.expression.terms.function.java.Sum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public class Function<T> {
    protected final String name;
    protected final String[] argNames; // argument names
    private final Type[] argTypes; // argument types
    private final Type returnType;
    private final boolean evaluable;
    private final boolean keyword; // indicates if we should omit () when calling this function with no args (ex: current_date)
    private final boolean aggregate;

    private static final Map<String, Function> functions = new HashMap<>();

    static {
        new Function("array").register();
        new Function("abs").register();
        new Function("CURRENT_DATE", PrimType.LOCAL_DATE, null, true).register();
        new Function("now", PrimType.LOCAL_DATE).register();
        new Function("date_trunc", PrimType.LOCAL_DATE).register();
        new Function("date_part", PrimType.INTEGER).register();
        new Function("to_char", PrimType.STRING).register();
        new Function("lower", PrimType.STRING).register();
        new Function("char_length", PrimType.INTEGER).register();
        new Function("count", PrimType.LONG).register();
        new Function("min").register();
        new Function("max").register();
        new Function("nullif").register();
        new Function("greatest").register();
        new Function("least").register();
        new Function("jsonb_build_array").register();

        // The caller, resolved by the server at execution and bound as a parameter — never as a literal,
        // because compiled SQL is cached by statement text and two callers send the same text.
        new ServerParameterFunction<>("CALLER_PERSON", "caller.person", PrimType.LONG).register();
        new ServerParameterFunction<>("CALLER_ACCOUNT", "caller.account", PrimType.LONG).register();
        // The caller's READ SCOPE, flattened by the server into one array each: the events their grants
        // cover, and the organisations they hold whole. Arrays rather than a value per grant because the
        // predicate is ANDed into somebody else's statement and cannot grow a clause per row, and because
        // an empty array is false for every row - the floor a caller with no grants lands on, which
        // `in ()` could not even compile.
        new ServerParameterFunction<>("CALLER_GRANTED_EVENTS", "caller.grantedEvents", PrimType.LONG, true).register();
        new ServerParameterFunction<>("CALLER_GRANTED_ORGANIZATIONS", "caller.grantedOrganizations", PrimType.LONG, true).register();

        new Sum().register();
        new StringAgg().register();
        new CurrentDate().register();

        new InlineFunction("readOnly", "e", null, "e").register();
        new InlineFunction("image", "src", new Type[]{SpecializedTextType.IMAGE_URL}, "src").register();
        new InlineFunction("html", "text", new Type[]{SpecializedTextType.HTML}, "text").register();
        new InlineFunction("isSet", "s", new Type[]{PrimType.STRING}, "s!=null and s!=''").register();
        new InlineFunction("isNotSet", "s", new Type[]{PrimType.STRING}, "s=null or s=''").register();
        new InlineFunction("oneOrZero", "b", new Type[]{PrimType.BOOLEAN}, "b ? 1 : 0").register();
        new InlineFunction("oneOrNull", "b", new Type[]{PrimType.BOOLEAN}, "b ? 1 : null").register();
        new InlineFunction("countIf", "b", new Type[]{PrimType.BOOLEAN}, "count(oneOrNull(b))").register();

        new Coalesce().register();
    }

    public static void register(Function function) {
        functions.put(function.name, function);
    }

    public static <T> Function<T> getFunction(String functionName) {
        return functions.get(functionName);
    }

    public Function(String name) {
        this(name, false);
    }

    public Function(String name, boolean keyword) {
        this(name, null, null, keyword);
    }

    public Function(String name, Type returnType) {
        this(name, returnType, null, false);
    }

    public Function(String name, Type returnType, Boolean evaluable, boolean keyword) {
        this(name, null, null, returnType, evaluable, keyword);
    }

    public Function(String name, String[] argNames, Type[] argTypes, Type returnType) {
        this(name, argNames, argTypes, returnType, null);
    }

    public Function(String name, String[] argNames, Type[] argTypes, Type returnType, Boolean evaluable) {
        this(name, argNames, argTypes, returnType, evaluable, false);
    }

    public Function(String name, String[] argNames, Type[] argTypes, Type returnType, Boolean evaluable, boolean keyword) {
        this.name = name;
        this.argNames = argNames;
        this.argTypes = argTypes;
        this.returnType = returnType;
        this.evaluable = evaluable != null ? evaluable : this instanceof AggregateFunction;
        this.keyword = keyword;
        // An aggregate makes SelectBuilder use row_number() as the id of a select without GROUP BY (a bare
        // id column next to an aggregate is invalid SQL). Before, only count was flagged: a CTE aggregating
        // with min/max (or sum, string_agg) alone compiled to "column x.id must appear in the GROUP BY
        // clause" (42803). Aggregate classes (Sum, StringAgg) are aggregates by type; SQL's built-in
        // aggregates registered as plain functions are recognised by name.
        aggregate = this instanceof AggregateFunction || isSqlAggregateName(name);
    }

    public String getName() {
        return name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public int getPrecedenceLevel() {
        return 9;
    }

    public boolean isEvaluable() {
        return evaluable;
    }

    public boolean isKeyword() {
        return keyword;
    }

    public boolean isAggregate() {
        return aggregate;
    }

    // SQL's built-in aggregate functions that may be registered as plain Functions (a switch rather than
    // Set.of, which this GWT-compiled module avoids).
    private static boolean isSqlAggregateName(String name) {
        if (name == null)
            return false;
        switch (name.toLowerCase()) {
            case "count":
            case "min":
            case "max":
            case "avg":
            case "bool_and":
            case "bool_or":
            case "array_agg":
                return true;
            default:
                return false;
        }
    }

    public boolean isSqlExpressible() {
        return !isEvaluable();
    }

    public boolean isIdentity() {
        return false;
    }

    public Object evaluate(T argument, DomainReader<T> domainReader) {
        throw new UnsupportedOperationException();
    }

    public void register() {
        register(this);
    }
}
