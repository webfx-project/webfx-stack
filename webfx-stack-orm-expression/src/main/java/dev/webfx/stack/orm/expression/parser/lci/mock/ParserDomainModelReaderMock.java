package dev.webfx.stack.orm.expression.parser.lci.mock;

import dev.webfx.stack.orm.expression.parser.lci.ParserDomainModelReader;
import dev.webfx.stack.orm.expression.terms.Symbol;
import dev.webfx.stack.orm.expression.parser.ExpressionParser;
import dev.webfx.extras.type.PrimType;
import dev.webfx.extras.type.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bruno Salmon
 */
public final class ParserDomainModelReaderMock implements ParserDomainModelReader {

    private final Map<String, String> fieldGroups = new HashMap<>();
    /** ClassName.fieldName => type. A field absent from here is not a field of this model. */
    private final Map<String, Type> fieldTypes = new HashMap<>();
    /** The type a field declared without one gets. */
    private Type defaultFieldType = PrimType.STRING;

    public ParserDomainModelReaderMock setFieldGroup(String name, String definition) {
        fieldGroups.put(name, definition);
        return this; // fluent API
    }

    /** Declare one field, typed. */
    public ParserDomainModelReaderMock declareField(Object domainClass, String fieldName, Type type) {
        fieldTypes.put(getFieldKey(domainClass, fieldName), type);
        return this; // fluent API
    }

    /** Declare several fields of one class at {@link #setDefaultFieldType(Type)}, comma-separated. */
    public ParserDomainModelReaderMock declareFields(Object domainClass, String commaSeparatedFieldNames) {
        for (String fieldName : commaSeparatedFieldNames.split(","))
            declareField(domainClass, fieldName.trim(), defaultFieldType);
        return this; // fluent API
    }

    public ParserDomainModelReaderMock setDefaultFieldType(Type type) {
        defaultFieldType = type;
        return this; // fluent API
    }

    private String getFieldKey(Object domainClass, String fieldName) {
        return domainClass + "." + fieldName;
    }

    @Override
    public Object getDomainClassByName(String name) {
        return name; // Domain classes are just names in this dummy model (no existence check and no additional info about them)
    }

    @Override
    public Symbol getDomainFieldSymbol(Object domainClass, String fieldName) {
        // Declared fields only, and NULL for anything else. This mock used to answer every name with an
        // untyped Symbol, which broke it two ways. FieldBuilder asks this reader BEFORE the reference
        // resolver, so answering everything shadowed every alias, CTE name and inline-function argument —
        // including while Function's static registrations lazily parse their bodies, which happens in the
        // middle of whatever parse first touches Function. And an untyped Symbol throws in the compiler,
        // which asks every symbol for its type. Returning null lets the resolver have the name instead.
        //
        // It cannot ask the resolver itself whether it knows the name: the resolvers build fields through
        // this reader, so consulting them here recurses until the stack ends.
        Type type = fieldTypes.get(getFieldKey(domainClass, fieldName));
        return type == null ? null : new Symbol(fieldName, type);
    }

    @Override
    public Symbol getDomainFieldGroupSymbol(Object domainClass, String fieldGroupName) {
        String fieldGroupDefinition = fieldGroups.get(fieldGroupName);
        if (fieldGroupDefinition != null)
            return new Symbol('<' + fieldGroupName + '>', ExpressionParser.parseExpression(fieldGroupDefinition, domainClass, this));
        return null;
    }

    @Override
    public Object getSymbolForeignDomainClass(Object symbolDomainClass, Symbol symbol) {
        String name = symbol.getName(); // term name, ex: "event" (in a dot navigation like event.name)
        return Character.toUpperCase(name.charAt(0)) + name.substring(1); // return "Event" as the foreign class
    }
}
