package dev.webfx.stack.orm.entity.result;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.db.submit.GeneratedKeyBatchIndex;
import dev.webfx.stack.db.submit.SubmitArgument;
import dev.webfx.stack.db.submit.SubmitResult;
import dev.webfx.stack.orm.domainmodel.DataSourceModel;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.dql.sqlcompiler.ExpressionSqlCompiler;
import dev.webfx.stack.orm.dql.sqlcompiler.lci.CompilerDomainModelReader;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.SqlCompiled;
import dev.webfx.stack.orm.dql.sqlcompiler.sql.dbms.DbmsSqlSyntax;
import dev.webfx.stack.orm.entity.EntityId;
import dev.webfx.stack.orm.entity.EntityStore;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.parser.lci.ParserDomainModelReader;
import dev.webfx.stack.orm.expression.terms.*;

import java.util.*;

/**
 * @author Bruno Salmon
 */
public final class EntityChangesToSubmitBatchGenerator {

    private EntityChangesToSubmitBatchGenerator() {
    }

    public static BatchGenerator createSubmitBatchGenerator(EntityChanges changes, DataSourceModel dataSourceModel, DataScope dataScope, SubmitArgument... initialUpdates) {
        return createSubmitBatchGenerator(changes, dataSourceModel.getDataSourceId(), dataScope, dataSourceModel.getDbmsSqlSyntax(), dataSourceModel.getDomainModel().getParserDomainModelReader(), dataSourceModel.getCompilerDomainModelReader(), initialUpdates);
    }

    public static BatchGenerator createSubmitBatchGenerator(EntityChanges changes, Object dataSourceId, DataScope dataScope, DbmsSqlSyntax dbmsSyntax, ParserDomainModelReader parserModelReader, CompilerDomainModelReader compilerModelReader, SubmitArgument... initialUpdates) {
        return new BatchGenerator(changes, dataSourceId, dataScope, dbmsSyntax, compilerModelReader, initialUpdates);
    }

    public static Batch<SubmitArgument> generateSubmitBatch(EntityChanges changes, DataSourceModel dataSourceModel, DataScope dataScope, SubmitArgument... initialUpdates) {
        return createSubmitBatchGenerator(changes, dataSourceModel, dataScope, initialUpdates).generate();
    }

    public static Batch<SubmitArgument> generateSubmitBatch(EntityChanges changes, Object dataSourceId, DataScope dataScope, DbmsSqlSyntax dbmsSyntax, ParserDomainModelReader parserModelReader, CompilerDomainModelReader compilerModelReader, SubmitArgument... initialUpdates) {
        return createSubmitBatchGenerator(changes, dataSourceId, dataScope, dbmsSyntax, parserModelReader, compilerModelReader, initialUpdates).generate();
    }

    public static final class BatchGenerator {

        final static Expression<?> WHERE_ID_EQUALS_PARAM = new Equals<>(IdExpression.singleton, Parameter.UNNAMED_PARAMETER);

        private final EntityChanges changes;
        private final Object dataSourceId;
        private final DataScope dataScope;
        private final DbmsSqlSyntax dbmsSyntax;
        private final CompilerDomainModelReader compilerModelReader;
        private final List<SubmitArgument> submitArguments;
        private final Map<EntityId, Integer> newEntityIdIndexInBatch = new IdentityHashMap<>();

        BatchGenerator(EntityChanges changes, Object dataSourceId, DataScope dataScope, DbmsSqlSyntax dbmsSyntax, CompilerDomainModelReader compilerModelReader, SubmitArgument... initialUpdates) {
            submitArguments = initialUpdates == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(initialUpdates));
            this.changes = changes;
            this.dataSourceId = dataSourceId;
            this.dataScope = dataScope;
            this.dbmsSyntax = dbmsSyntax;
            this.compilerModelReader = compilerModelReader;
        }

        public Batch<SubmitArgument> generate() {
            // First generating delete statements
            generateDeletes();
            // Then insert and update statements. Statements parameters values may temporary contains EntityId objects,
            // which will be replaced on next step while sorting statements.
            generateInsertUpdates();
            // Finally sorting the statements so that any statement (insert or update) that is referring to a new entity
            // will be executed after that entity has been inserted into the database. For such statements, the parameter
            // value referring to the new entity is replaced with a GeneratedKeyBatchIndex object that contains the index
            // of the insert statement in the batch. The SubmitService must replace that value with the generated key
            // returned by that insert statement (which will be already executed at this stage thanks to the sort).
            // This sorting method also replaces all other (not new) EntityId with their primary key in the parameter
            // values so that they can be used as is without any transformation by the SubmitService.
            sortStatementsByCreationOrder();
            // Grouping by batch
            groupStatementsByBatch();
            // Returning the batch
            return new Batch<>(submitArguments.toArray(new SubmitArgument[0]));
        }

        public void applyGeneratedKeys(Batch<SubmitResult> ar, EntityStore store) {
            // Updating the Ids of the entities newly created with the generated keys returned by the database
            for (Map.Entry<EntityId, Integer> entry : newEntityIdIndexInBatch.entrySet()) {
                EntityId newEntityId = entry.getKey();
                Integer indexInBatch = entry.getValue();
                SubmitResult submitResult = ar.get(indexInBatch);
                Object generatedKey = submitResult.getGeneratedKeys()[0];
                store.applyEntityIdRefactor(newEntityId, generatedKey);
            }
        }

        void sortStatementsByCreationOrder() {
            int size = submitArguments.size();
            // sortedList will be temporarily used to sort the SubmitArguments, and once finished, will be copied back to submitArguments
            List<SubmitArgument> sortedList  = new ArrayList<>(Collections.nCopies(size, null)); // correct size already, but initially filled with null
            // This second list will memorize the index translation of the SubmitArguments (index in initial list => index in the sorted list)
            List<Integer> newEntityIndexTranslationAfterSort = new ArrayList<>(Collections.nCopies(size, null)); // correct size already, but initially filled with null
            boolean indexChanged = false; // flag to skip the final index update loop if not necessary (optimization)
            int sortedIndex = 0; // Index of future sorted statement
            while (sortedIndex < size) { // means the sort is not finished
                // We are looking for the next statements with parameters resolved (i.e. value = literal or a newly
                // generated id that is coming from a previous statement already sorted)
                boolean someResolved = false; // will be set to true once we found one, which should happen unless cyclic references are present
                loop: // iterating over all submitArguments not yet sorted
                for (int index = 0; index < size; index++) {
                    SubmitArgument arg = submitArguments.get(index);
                    if (arg != null) { // a null value means it has been sorted already
                        Object[] parameters = arg.getParameters(); // the parameters to check if they are resolved or not
                        // We iterate over these parameters
                        for (int parameterIndex = 0, length = Arrays.length(parameters); parameterIndex < length; parameterIndex++) {
                            Object value = parameters[parameterIndex];
                            if (value instanceof EntityId) { // means it's a reference to another entity (and not a literal value)
                                // In the process, we also replace EntityIds with primary keys (preparing for SQL parameters)
                                EntityId entityId = (EntityId) value;
                                if (!entityId.isNew()) // means the pk already exists in the database, so we can use it
                                    parameters[parameterIndex] = entityId.getPrimaryKey(); // straightaway
                                else { // otherwise, we don't know in advance the value before the database generates it
                                    // (generated key). In that case, we replace it with a GeneratedKeyBatchIndex instance
                                    // which will indicate the index of the previous statement in the batch that the
                                    // server will need to take the value from (which will be generated at this time).
                                    // To do that, we first get its initial index (before this sort).
                                    Integer initialIndex = newEntityIdIndexInBatch.get(entityId);
                                    // Then we get its new index in the sorted list (will be null if not yet resolved)
                                    Integer finalIndex = initialIndex == null ? null : newEntityIndexTranslationAfterSort.get(initialIndex);
                                    if (finalIndex == null) // means that this parameter value is not yet resolved
                                        continue loop; // therefore, we go to the next statement (this one can't be processed right now)
                                    // If we get the final index, we record it inside a GeneratedKeyBatchIndex instance
                                    parameters[parameterIndex] = new GeneratedKeyBatchIndex(finalIndex);
                                }
                            }
                        }
                        // If we reach this point, it means that there are no unresolved parameters for this
                        // submitArgument and therefore, it is suitable to be executed as next in the sorted list.
                        submitArguments.set(index, null); // Moving the submitArgument from the original list
                        sortedList.set(sortedIndex, arg); // to the sorted list
                        // and memorising the index translation between the 2 lists
                        newEntityIndexTranslationAfterSort.set(index, sortedIndex);
                        if (sortedIndex != index)
                            indexChanged = true;
                        // Indicating that we found at least one statement that was resolved
                        someResolved = true;
                        // Preparing for the next iteration
                        sortedIndex++;
                    }
                }
                // If none of the submitArgument could be resolved, we are stuck and can't sort the list anymore
                if (!someResolved) // this happens when there are cyclic references, which we complain about
                    throw new IllegalStateException("Cyclic references detected");
            }
            // Applying the result of the sort to the original list
            for (int i = 0; i < size; i++)
                submitArguments.set(i, sortedList.get(i));
            // And to the indexes (if they have changed)
            if (indexChanged) {
                for (Map.Entry<EntityId, Integer> entry : newEntityIdIndexInBatch.entrySet()) {
                    Integer initialIndex = entry.getValue();
                    Integer finalIndex = newEntityIndexTranslationAfterSort.get(initialIndex);
                    entry.setValue(finalIndex);
                }
            }
        }

        void groupStatementsByBatch() {

        }


        void generateDeletes() {
            Collection<EntityId> deletedEntities = changes.getDeletedEntityIds();
            if (deletedEntities != null && !deletedEntities.isEmpty()) {
                /* Commented delete sort (not working), so for now the application code is responsible for sequencing deletes
                List<EntityId> deletedList = new ArrayList<>(deletedEntities);
                // Sorting according to classes references
                deletedList.sort(comparing(id -> id.getDomainClass().getName()));
                */
                deletedEntities.forEach(this::generateDelete);
            }
        }

        void generateDelete(EntityId id) {
            Delete<?> delete = new Delete<>(id.getDomainClass(), null, WHERE_ID_EQUALS_PARAM);
            addToBatch(delete, id.getPrimaryKey());
        }

        void generateInsertUpdates() {
            EntityResult rs = changes.getInsertedUpdatedEntityResult();
            if (rs != null) {
                for (EntityId id : rs.getEntityIds()) {
                    List<Equals<?>> assignments = new ArrayList<>();
                    List<Object> values = new ArrayList<>();
                    for (Object fieldId : rs.getFieldIds(id))
                        if (fieldId != null) {
                            DomainField field = id.getDomainClass().getField(fieldId);
                            assignments.add(new Equals(field, Parameter.UNNAMED_PARAMETER));
                            values.add(rs.getFieldValue(id, fieldId));
                        }
                    if (assignments.isEmpty() && !id.isNew())
                        continue;
                    ExpressionArray<?> setClause = new ExpressionArray(assignments);
                    if (id.isNew()) { // insert statement
                        newEntityIdIndexInBatch.put(id, submitArguments.size());
                        Insert<?> insert = new Insert(id.getDomainClass(), setClause);
                        addToBatch(insert, values.isEmpty() ? null : values.toArray());
                    } else { // update statement
                        Update<?> update = new Update(id.getDomainClass(), setClause, WHERE_ID_EQUALS_PARAM);
                        values.add(id.getPrimaryKey());
                        addToBatch(update, values.toArray());
                    }
                }
            }
        }

        private static final boolean USE_DQL_LANGUAGE = true;

        void addToBatch(DqlStatement<?> dqlStatement, Object... parameterValues) {
            if (USE_DQL_LANGUAGE)
                addToBatch("DQL", dqlStatement.toString(), parameterValues);
            else
                addToBatch(ExpressionSqlCompiler.compileStatement(dqlStatement, dbmsSyntax, compilerModelReader), parameterValues);
        }

        void addToBatch(SqlCompiled sqlcompiled, Object... parameters) {
            addToBatch(null, sqlcompiled.getSql(), parameters);
        }

        void addToBatch(String submitLang, String submitString, Object... parameters) {
            submitArguments.add(SubmitArgument.builder()
                    .setDataSourceId(dataSourceId)
                    .setDataScope(dataScope)
                    .setLanguage(submitLang)
                    .setStatement(submitString)
                    .setParameters(parameters)
                    .build());
        }
    }
}
