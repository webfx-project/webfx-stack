package dev.webfx.stack.db.query.buscall;

import dev.webfx.stack.com.bus.call.spi.AsyncFunctionBusCallEndpoint;
import dev.webfx.stack.db.query.QueryService;

/**
 * Bus endpoint that provides class ID to name mappings from the domain model.
 * This bridges the query service and domain model service to avoid circular dependencies.
 *
 * @author Bruno Salmon
 */
public final class LoadClassMappingsBusCallEndpoint extends AsyncFunctionBusCallEndpoint<Object, Object[][]> {

    public LoadClassMappingsBusCallEndpoint() {
        super(QueryServiceBusAddress.LOAD_CLASS_MAPPINGS_METHOD_ADDRESS, QueryService::loadClassMappings);
    }

}
