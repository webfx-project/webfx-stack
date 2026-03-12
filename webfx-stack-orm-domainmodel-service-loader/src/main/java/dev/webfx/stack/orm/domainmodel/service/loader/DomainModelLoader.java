package dev.webfx.stack.orm.domainmodel.service.loader;

import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.console.Console;
import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.stack.db.query.QueryService;
import dev.webfx.extras.type.DerivedType;
import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.type.Type;
import dev.webfx.platform.util.Numbers;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.orm.domainmodel.DomainModel;
import dev.webfx.stack.orm.domainmodel.builder.DomainClassBuilder;
import dev.webfx.stack.orm.domainmodel.builder.DomainFieldBuilder;
import dev.webfx.stack.orm.domainmodel.builder.DomainFieldsGroupBuilder;
import dev.webfx.stack.orm.domainmodel.builder.DomainModelBuilder;
import dev.webfx.extras.label.Label;
import dev.webfx.stack.db.query.QueryArgument;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class DomainModelLoader {
    private final Object id;
    private final DomainModelBuilder dmb;
    private final Object dataSourceId = 0;
    private final Map<Object /*classId*/, DomainClassBuilder> classes = new HashMap<>();
    private final Map<Object /* id */, Type> typeMap = new HashMap<>();
    private final Map<Object /* id */, Label> labelMap = new HashMap<>();

    public DomainModelLoader(Object id) {
        dmb = new DomainModelBuilder(id);
        this.id = id;
    }

    public Future<DomainModel> loadDomainModel() {
        return QueryService.executeQueryBatch(generateDomainModelQueryBatch()).map(this::generateDomainModel);
    }

    public Batch<QueryArgument> generateDomainModelQueryBatch() {
        // Note: we use ? and not $1 for the parameterized queries because this method is also called by the generator
        // DomainModelSnapshotTaker (from kbs2-to-modality-modelimport project) which connects to the KBS2 HSQL database
        // using JDBC (and JDBC recognizes ? as parameter placeholder but not $1)
        return toQueryBatch(
                // 1) Labels loading
                "select id,code,text,icon from label where data_model_version_id=? or data_model_version_id is null",
                // 2) Types loading
                "select id,name,super_type_id,cell_factory_name,ui_format,sql_format from type where data_model_version_id=?",
                // 3) Classes loading
                "select id,name,sql_table_name,foreign_fields,fxml_form,search_condition,label_id,deprecated from class where data_model_version_id=?",
                // 4) Style classes loading
                "select c.id,f.name,s.name,s.condition from data_view s join data_view f on f.id=s.parent_id join class c on c.id=f.scope_class_id where c.data_model_version_id=? and active and is_style and not is_folder and s.scope_activity_id is null order by c.id,f.ord,s.ord desc",
                // 5) Fields loading
                "select id,name,class_id,type_id,label_id,pref_width,expression,applicable_condition,persistent,sql_column_name,foreign_class_id,foreign_alias,foreign_condition,foreign_order_by,foreign_combo_fields,foreign_table_fields,deprecated from field f join class c on f.class_id=c.id where c.data_model_version_id=?",
                // 6) Fields group loading
                "select name,class_id,fields from fields_group fg join class c on fg.class_id=c.id where c.data_model_version_id=?"
        );
    }

    private Batch<QueryArgument> toQueryBatch(String... queryStrings) {
        QueryArgument[] args = new QueryArgument[queryStrings.length];
        for (int i = 0; i < queryStrings.length; i++)
            args[i] = toQueryArgument(queryStrings[i]);
        return new Batch<>(args);
    }

    private QueryArgument toQueryArgument(String queryString) {
        return QueryArgument.builder()
                .setStatement(queryString)
                .setParameters(id)
                .setDataSourceId(dataSourceId)
                .build();
    }

    public DomainModel generateDomainModel(Batch<QueryResult> batchResult) {
        long t0 = System.currentTimeMillis();
        QueryResult[] results = batchResult.getArray();

        // 1) Building labels
        QueryResult rs = results[0];
        for (int row = 0; row < rs.getRowCount(); row++)
            labelMap.put(rs.getValue(row, 0 /*"id"*/), new Label(rs.getValue(row, 1 /*"code"*/), rs.getValue(row, 2 /*"text"*/), rs.getValue(row, 3 /*"icon"*/)));

        // 2) Building types
        rs = results[1];
        for (int row = 0; row < rs.getRowCount(); row++) {
            Object typeId = rs.getValue(row, 0 /*"id"*/);
            Type superType = getTypeFromId(rs.getValue(row, 2 /*"super_type_id"*/));
            //TextFieldFormat uiFormat = TextFieldFormat.parseDefinition(rs.getString("ui_format"));
            //TextFieldFormat sqlFormat = TextFieldFormat.parseDefinition(rs.getString("sql_format"));
            //typeMap.put(typeId, new Type(typeId, rs.getString("name"), superType, null, rs.getString("cell_factory_name"), null, null, uiFormat, sqlFormat));
            typeMap.put(typeId, DerivedType.create(rs.getValue(row, 1 /*"name"*/), superType));
        }

        // 3) Building classes
        rs = results[2];
        for (int row = 0; row < rs.getRowCount(); row++) {
            Object classId = rs.getValue(row, 0 /*"id"*/);
            final DomainClassBuilder classBuilder = dmb.newClassBuilder(rs.getValue(row, 1 /*"name"*/), true);
            classBuilder.id = classId;
            classBuilder.sqlTableName = rs.getValue(row, 2 /*"sql_table_name"*/);
            classBuilder.foreignFieldsDefinition = rs.getValue(row, 3 /*"foreign_fields"*/);
            classBuilder.fxmlForm = rs.getValue(row, 4 /*"fxml_form"*/);
            classBuilder.searchCondition = rs.getValue(row, 5 /*"search_condition"*/);
            //classBuilder.css = rs.getString("css");
            classBuilder.label = labelMap.get(rs.getValue(row, 6 /*"label_id"*/));
            classBuilder.deprecated = rs.getBoolean(row, 7 /*deprecated*/, false);
            classes.put(classId, classBuilder);
        }

        // 4) Style classes loading
        rs = results[3];
        StringBuilder allDefinitions = null;
        String currentDefinition = null;
        String folderName = null;
        Object lastClassId = null;
        for (int row = 0; row < rs.getRowCount(); row++) {
            Object classId = rs.getValue(row, 0);
            String fName = rs.getValue(row, 1);
            String styleName = rs.getValue(row, 2);
            String condition = rs.getValue(row, 3);
            if (lastClassId == null || !lastClassId.equals(classId)) { // Going to next class
                if (lastClassId != null)
                    recordStyleClassesExpressionArrayDefinition(classes.get(lastClassId), allDefinitions, currentDefinition);
                allDefinitions = null;
                currentDefinition = null;
                folderName = null;
                lastClassId = classId;
            }
            if (folderName != null && !folderName.equals(fName)) { // Going to next folder
                allDefinitions = appendDefinition(currentDefinition, allDefinitions);
                currentDefinition = null;
            }
            folderName = fName;
            if (condition == null) // should arrive in first position due to order by desc ordering (the null condition is the last one)
                currentDefinition = "'" + styleName + "'";
            else {
                /*Object activityId = rs.getValue(row, 4);
                if (activityId != null) // if scope_activity is set, adding this criteria in the condition:
                    e = new And(e, new Equals(new Parameter("selectedActivity", null), Constant.newConstant(activityId)));*/
                currentDefinition = '(' + condition + ") ? '" + styleName + "' : " + currentDefinition;
            }
        }
        if (currentDefinition != null)
            recordStyleClassesExpressionArrayDefinition(classes.get(lastClassId), allDefinitions, currentDefinition);

        // 5) Building fields
        rs = results[4];
        for (int row = 0; row < rs.getRowCount(); row++) {
            Object typeId = rs.getValue(row, 3 /*"type_id"*/);
            Type type = getTypeFromId(typeId);
            DomainClassBuilder classBuilder = classes.get(rs.getValue(row, 2 /*"class_id"*/));
            DomainFieldBuilder fieldBuilder = classBuilder.newFieldBuilder(rs.getValue(row, 1 /*"name"*/), type, true);
            //CoreSystem.log("Building field " + classBuilder.name + '.' + fieldBuilder.name);
            fieldBuilder.modelId = rs.getValue(row, 0 /*"id"*/); // should be model_id (doesn't exist yet)
            fieldBuilder.label = labelMap.get(rs.getValue(row, 4 /*"label_id"*/));
            fieldBuilder.prefWidth = rs.getInt(row,5 /*"pref_width"*/, 0);
            fieldBuilder.expressionDefinition = rs.getValue(row, 6 /*"expression"*/);
            fieldBuilder.applicableConditionDefinition = rs.getValue(row, 7 /*"applicable_condition"*/);
            fieldBuilder.persistent = rs.getBoolean(row, 8 /*"persistent"*/, false);
            fieldBuilder.foreignAlias = rs.getValue(row, 11 /*"foreign_alias"*/);
            fieldBuilder.foreignCondition = rs.getValue(row, 12 /*"foreign_condition"*/);
            fieldBuilder.foreignOrderBy = rs.getValue(row, 13 /*"foreign_order_by"*/);
            fieldBuilder.foreignComboFields = rs.getValue(row, 14 /*"foreign_combo_fields"*/);
            fieldBuilder.foreignTableFields = rs.getValue(row, 15 /*"foreign_table_fields"*/);
            fieldBuilder.deprecated = rs.getBoolean(row, 16, false);
            /* TODO : thinking about foreignKey management
            if (fieldBuilder.type != null && fieldBuilder.type.getBaseType() == BaseType.FOREIGN_KEY && rs.getObject("foreign_class_id") != null)
                fieldBuilder.type = new Type(classes.get(rs.getValue("foreign_class_id")).getObjClass()); */
            DomainClassBuilder foreignClassBuilder = classes.get(rs.getValue(row, 10 /*"foreign_class_id"*/));
            if (foreignClassBuilder != null)
                fieldBuilder.foreignClass = foreignClassBuilder.getDomainClass();
            fieldBuilder.sqlColumnName = rs.getValue(row, 9 /*"sql_column_name"*/);
        }

        // 6) Building fields groups
        rs = results[5];
        for (int row = 0; row < rs.getRowCount(); row++) {
            DomainClassBuilder classBuilder = classes.get(rs.getValue(row, 1 /*"class_id"*/));
            DomainFieldsGroupBuilder groupBuilder = classBuilder.newFieldsGroupBuilder(rs.getValue(row, 0 /*"name"*/), true);
            groupBuilder.fieldsDefinition = rs.getValue(row, 2 /*"fields"*/);
        }
        Console.log("Domain model loaded: " + results[2].getRowCount() + " classes, " + results[4].getRowCount() + " fields, " + results[5].getRowCount() + " fields groups and " + results[0].getRowCount() + " labels in " + (System.currentTimeMillis() - t0) + " ms");
        // Building and returning final domain model
        return dmb.build();
    }

    private Type getTypeFromId(Object typeId) {
        Type type = typeMap.get(typeId);
        if (type == null)
            type = getPrimTypeFromId(typeId);
        return type;
    }

    private PrimType getPrimTypeFromId(Object id) {
        if (id == null)
            return null;
        // is there anything else?
        return switch (Numbers.intValue(id)) { // Keeping compatibility with KBS2.0 types
            case 0 -> PrimType.INTEGER;
            case 1 -> PrimType.LONG;
            case 2 -> PrimType.FLOAT;
            case 3 -> PrimType.DOUBLE;
            case 4 -> PrimType.BOOLEAN;
            case 5 -> PrimType.STRING;
            case 6 -> PrimType.LOCAL_DATE;
            case 7 -> PrimType.LONG; // FOREIGN_KEY
            case 105 -> PrimType.LOCAL_DATE_TIME;
            case 106 -> PrimType.LOCAL_TIME;
            case 107 -> PrimType.INSTANT;
            case 108 -> PrimType.YEAR_MONTH;
            default -> throw new IllegalArgumentException(); // is there anything else?
        };
    }

    private static StringBuilder appendDefinition(String definition, StringBuilder allDefinitions) {
        if (definition != null) {
            if (allDefinitions == null)
                allDefinitions = new StringBuilder(definition);
            else
                allDefinitions.append(',').append(definition);
        }
        return allDefinitions;
    }

    private static void recordStyleClassesExpressionArrayDefinition(DomainClassBuilder classBuilder, StringBuilder allDefinitions, String lastDefinition) {
        String finalExpressionDefinition;
        if (allDefinitions == null)
            finalExpressionDefinition = lastDefinition;
        else
            finalExpressionDefinition = appendDefinition(lastDefinition, allDefinitions).toString();
        classBuilder.styleClassesExpressionArrayDefinition = finalExpressionDefinition;
    }

    /**
     * Build a simplified AST representation of the domain model for AI agent consumption.
     *
     * The returned object has the shape:
     * <pre>
     * {
     *   "ClassName": {
     *     "fieldName": "string" | "boolean" | "integer" | "long" | "date" | "ForeignClassName",
     *     "exprFieldName": { "expr": "field1 - field2" },
     *     ...
     *   },
     *   ...
     * }
     * </pre>
     *
     * Rules:
     * - Persistent DB fields: value is a type string (e.g., "string", "date", "Organization")
     * - Expression fields: value is an object { "expr": "&lt;dsql-expression&gt;" } — the server
     *   expands these to their constituent columns at query time; the client is expected to
     *   evaluate the expression to reconstruct the computed field value
     * - Non-persistent fields with no expression definition are omitted
     * - The implicit "id" primary-key field is omitted (every entity has it)
     * - Classes and fields are emitted in alphabetical order
     *
     * Must be called after {@link #generateDomainModel(Batch)} has been invoked.
     */
    public AstObject buildSimplifiedAstModel() {
        AstObject root = AST.createObject();
        classes.values().stream()
            .distinct()
            .sorted(Comparator.comparing(cb -> cb.name))
            .forEach(classBuilder -> {
                if (classBuilder.deprecated)
                    return;
                AstObject fields = AST.createObject();
                fields.set("id", classBuilder.id);
                classBuilder.fieldMap.values().stream()
                    .sorted(Comparator.comparing(fb -> fb.name))
                    .forEach(fieldBuilder -> {
                        if (fieldBuilder.deprecated)
                            return;
                        if (fieldBuilder.expressionDefinition != null) {
                            // Expression field (no DB column): emit { "expr": "price_net - price_deposit" }
                            // The server expands these to their constituent columns at query time;
                            // the client must evaluate the expression to reconstruct the field value.
                            AstObject exprObj = AST.createObject();
                            exprObj.set("expr", fieldBuilder.expressionDefinition);
                            fields.set(fieldBuilder.name, exprObj);
                        } else if (fieldBuilder.persistent) {
                            // Persistent DB field: emit the type string
                            String typeStr = resolveSimplifiedType(fieldBuilder);
                            if (typeStr != null)
                                fields.set(fieldBuilder.name, typeStr);
                        }
                        // else: non-persistent with no expression (edge case) — omit
                    });
                root.set(classBuilder.name, fields);
            });
        return root;
    }

    /**
     * Resolve the simplified type string for a field:
     * - Foreign-key fields → target class name (e.g., "Organization")
     * - Primitive fields   → lowercase primitive name (e.g., "string", "date")
     * - Derived types      → derived type name (e.g., "html")
     * - Unknown            → null (field will be omitted)
     */
    private static String resolveSimplifiedType(DomainFieldBuilder fb) {
        // Foreign-key field: emit the target class name
        if (fb.foreignClass != null)
            return fb.foreignClass.getName();
        // Primitive types: emit a lowercase, JS-friendly name
        if (fb.type instanceof PrimType) {
            return switch ((PrimType) fb.type) {
                case STRING -> "string";
                case BOOLEAN -> "boolean";
                case BYTE, SHORT, INTEGER -> "integer";
                case LONG -> "long";
                case FLOAT -> "float";
                case DOUBLE -> "double";
                case LOCAL_DATE -> "Temporal.PlainDate";
                case LOCAL_DATE_TIME -> "Temporal.PlainDateTime";
                case LOCAL_TIME -> "Temporal.PlainTime";
                case INSTANT -> "Temporal.Instant";
                case YEAR_MONTH -> "Temporal.PlainYearMonth";
            };
        }
        // Derived types (e.g., "html", "image"): emit the derived type name
        if (fb.type instanceof DerivedType)
            return ((DerivedType) fb.type).getName();
        return null; // unknown type — omit the field
    }
}
