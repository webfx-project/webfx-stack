package dev.webfx.stack.orm.domainmodel.service;

import dev.webfx.stack.orm.domainmodel.DomainModel;
import dev.webfx.stack.orm.domainmodel.service.spi.DomainModelProvider;
import dev.webfx.platform.async.Future;
import dev.webfx.platform.util.serviceloader.SingleServiceProvider;

import java.util.ServiceLoader;

/**
 * @author Bruno Salmon
 */
public final class DomainModelService {

    public static DomainModelProvider getProvider() {
        return SingleServiceProvider.getProvider(DomainModelProvider.class, () -> ServiceLoader.load(DomainModelProvider.class));
    }

    public static Future<DomainModel> loadDomainModel(Object dataSourceId) {
        return getProvider().loadDomainModel(dataSourceId);
    }

}
