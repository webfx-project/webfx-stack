package dev.webfx.stack.db.query.spi;

import dev.webfx.stack.db.query.QueryResult;
import dev.webfx.platform.async.Batch;
import dev.webfx.platform.async.Future;
import dev.webfx.stack.db.query.QueryArgument;
import dev.webfx.stack.orm.domainmodel.DomainClass;
import dev.webfx.stack.orm.domainmodel.service.DomainModelService;

/**
 * @author Bruno Salmon
 */
public interface QueryServiceProvider {

    Future<QueryResult> executeQuery(QueryArgument argument);

    // Batch support

    default Future<Batch<QueryResult>> executeQueryBatch(Batch<QueryArgument> batch) {
        return batch.executeParallel(QueryResult[]::new, this::executeQuery);
    }

    // Domain model class mappings (info required for the React clients)

    default Future<Object[][]> loadClassMappings(Object dataSourceId) {
        return DomainModelService.loadDomainModel(dataSourceId)
            .map(domainModel -> {
                // Extract class ID -> name mappings
                java.util.List<DomainClass> classes = domainModel.getAllClasses();
                Object[][] mappings = new Object[classes.size()][2];
                for (int i = 0; i < classes.size(); i++) {
                    var domainClass = classes.get(i);
                    mappings[i][0] = domainClass.getId();        // class ID (numeric)
                    mappings[i][1] = domainClass.getName();      // class name (string)
                }
                return mappings;
            });
    }

}
