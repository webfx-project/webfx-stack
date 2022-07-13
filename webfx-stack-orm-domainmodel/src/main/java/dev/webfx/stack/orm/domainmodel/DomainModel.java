package dev.webfx.stack.orm.domainmodel;

import dev.webfx.stack.orm.domainmodel.lciimpl.ParserDomainModelReaderImpl;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.parser.ExpressionParser;
import dev.webfx.stack.orm.expression.terms.DqlStatement;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import dev.webfx.stack.orm.expression.terms.Select;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Bruno Salmon
 */
public final class DomainModel {

    private final Object id;
    private final Map<Object /* id, modelId, name or sqlTable */, DomainClass> classMap;
    private final ParserDomainModelReaderImpl parserDomainModelReader = new ParserDomainModelReaderImpl(this);

    public DomainModel(Object id, Map<Object, DomainClass> classMap) {
        this.id = id;
        this.classMap = classMap;
    }

    public Object getId() {
        return id;
    }

    public DomainClass getClass(Object classId) {
        return classMap.get(classId);
    }

    public List<DomainClass> getAllClasses() {
        return classMap.values().stream()
                .distinct()
                .sorted(Comparator.comparing(DomainClass::getName))
                .collect(Collectors.toList());
    }

    public ParserDomainModelReaderImpl getParserDomainModelReader() {
        return parserDomainModelReader;
    }

    public <T> Expression<T> parseExpression(String definition, Object classId) {
        return ExpressionParser.parseExpression(definition, classId, parserDomainModelReader);
    }

    public <T> ExpressionArray<T> parseExpressionArray(String definition, Object classId) {
        return ExpressionParser.parseExpressionArray(definition, classId, parserDomainModelReader);
    }

    public <T> Select<T> parseSelect(String definition) {
        return ExpressionParser.parseSelect(definition, parserDomainModelReader);
    }

    public <T> DqlStatement<T> parseStatement(String definition) {
        return ExpressionParser.parseStatement(definition, parserDomainModelReader);
    }
}
