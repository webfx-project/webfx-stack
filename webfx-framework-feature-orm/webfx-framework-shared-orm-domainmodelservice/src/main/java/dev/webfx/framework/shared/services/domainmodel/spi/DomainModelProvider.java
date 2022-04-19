package dev.webfx.framework.shared.services.domainmodel.spi;

import dev.webfx.framework.shared.orm.domainmodel.DomainModel;
import dev.webfx.platform.shared.async.Future;

/**
 * @author Bruno Salmon
 */
public interface DomainModelProvider {

    Future<DomainModel> loadDomainModel(Object dataSourceId);

}
