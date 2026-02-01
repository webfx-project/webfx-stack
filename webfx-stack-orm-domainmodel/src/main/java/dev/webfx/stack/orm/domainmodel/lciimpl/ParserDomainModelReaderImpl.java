package dev.webfx.stack.orm.domainmodel.lciimpl;

import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.domainmodel.DomainModel;
import dev.webfx.stack.orm.expression.terms.Symbol;
import dev.webfx.stack.orm.expression.parser.lci.ParserDomainModelReader;

/**
 * @author Bruno Salmon
 */
public final class ParserDomainModelReaderImpl implements ParserDomainModelReader {

    private final DomainModel domainModel;

    public ParserDomainModelReaderImpl(DomainModel domainModel) {
        this.domainModel = domainModel;
    }

    @Override
    public Object getDomainClassByName(String name) {
        return domainModel.getClass(name);
    }

    private DomainClass toDomainClass(Object domainClass) {
        if (domainClass instanceof DomainClass dc)
            return dc;
        DomainClass domainModelClass = domainModel.getClass(domainClass);
        if (domainModelClass == null)
            throw new IllegalArgumentException("Domain class '" + domainClass + "' not found in domain model");
        return domainModelClass;
    }

    @Override
    public Symbol getDomainFieldSymbol(Object domainClass, String fieldName) {
        return toDomainClass(domainClass).getFieldSilently(fieldName);
    }

    @Override
    public Symbol getDomainFieldGroupSymbol(Object domainClass, String fieldGroupName) {
        return toDomainClass(domainClass).getFieldsGroup(fieldGroupName);
    }

    @Override
    public Object getSymbolForeignDomainClass(Object symbolDomainClass, Symbol symbol) {
        if (symbol instanceof DomainField domainField)
            return domainField.getForeignClass();
        return null;
    }
}
