package dev.webfx.stack.framework.shared.services.domainmodel.spi;

import dev.webfx.stack.framework.shared.orm.domainmodel.DomainModel;
import dev.webfx.stack.platform.async.Future;

/**
 * @author Bruno Salmon
 */
public interface DomainModelProvider {

    Future<DomainModel> loadDomainModel(Object dataSourceId);

}
