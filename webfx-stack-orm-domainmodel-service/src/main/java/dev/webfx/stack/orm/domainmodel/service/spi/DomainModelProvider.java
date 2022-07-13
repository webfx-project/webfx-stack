package dev.webfx.stack.orm.domainmodel.service.spi;

import dev.webfx.stack.orm.domainmodel.DomainModel;
import dev.webfx.stack.async.Future;

/**
 * @author Bruno Salmon
 */
public interface DomainModelProvider {

    Future<DomainModel> loadDomainModel(Object dataSourceId);

}
