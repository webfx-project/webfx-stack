package dev.webfx.stack.orm.entity.result;

import dev.webfx.platform.async.Batch;
import dev.webfx.platform.util.Arrays;
import dev.webfx.stack.db.datascope.DataScope;
import dev.webfx.stack.db.submit.GeneratedKeyReference;
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
        private final Map<EntityId, Integer> newEntityIdIndexInGeneratedKeys = new IdentityHashMap<>();

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
            // value referring to the new entity is replaced with a GeneratedKeyReference object that contains the index
            // of the insert statement in the batch. The SubmitService must replace that value with the generated key
            // returned by that insert statement (which will be already executed at this stage thanks to the sort).
            // This sorting method also replaces all other (not new) EntityId with their primary key in the parameter
            // values so that they can be used as is without any transformation by the SubmitService.
            sortStatementsByDependencyOrder();
            // Grouping identical statements. Ex:
            // update X set f=? where id=? [p1, p2]
            // update X set f=? where id=? [p3, p4]
            // ...
            // => update X set f=? where id=? [Batch([p1, p2], [p3, p4], ...] (=> single call to database)
            groupIdenticalStatements();
            // Returning the batch of SubmitArguments
            return new Batch<>(submitArguments.toArray(new SubmitArgument[0]));
        }

        public void applyGeneratedKeys(Batch<SubmitResult> ar, EntityStore store) {
            // Updating the Ids of the entities newly created with the generated keys returned by the database
            for (Map.Entry<EntityId, Integer> entry : newEntityIdIndexInBatch.entrySet()) {
                EntityId newEntityId = entry.getKey();
                int indexInBatch = entry.getValue();
                SubmitResult submitResult = ar.get(indexInBatch);
                int generatedKeyIndex = newEntityIdIndexInGeneratedKeys.getOrDefault(newEntityId, 0);
                Object generatedKey = submitResult.getGeneratedKeys()[generatedKeyIndex];
                store.applyEntityIdRefactor(newEntityId, generatedKey);
            }
        }

        void sortStatementsByDependencyOrder() {
            // TODO: make an initial sort by statement here to optimize the chance of grouping after the dependency sort
            int size = submitArguments.size();
            // sortedList will be temporarily used to sort the SubmitArguments, and once finished, will be copied back to submitArguments
            List<SubmitArgument> sortedList = new ArrayList<>(Collections.nCopies(size, null)); // correct size already, but initially filled with null
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
                                    parameters[parameterIndex] = new GeneratedKeyReference(finalIndex);
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
                    throw new IllegalStateException("Missing entities or cyclic references detected");
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

        void groupIdenticalStatements() {
            // We create a list that will group all identical SubmitArgument (parameters will be grouped in a Batch)
            List<SubmitArgument> groupList = new ArrayList<>(submitArguments.size());
            List<Integer> batchIndexTranslations = new ArrayList<>(submitArguments.size());
            // Initial empty group (no sample) that will be seen as a group break with the first SubmitArgument in the loop
            SubmitArgument groupSample = null;
            List<Object[]> groupParameters = null;
            // We iterate over all submitArguments and try to group them
            for (SubmitArgument submitArgument : submitArguments) {
                // We try to add the submitArgument in the existing group (won't work with initial empty group)
                boolean added = addToExistingStatementGroup(submitArgument, groupSample, groupParameters);
                if (!added) { // => group break
                    // We record the possible existing group into the groupList (initial null will be skipped)
                    recordExistingStatementGroup(groupSample, groupParameters, groupList, batchIndexTranslations);
                    // and start a new group with this submitArgument as sample
                    groupSample = submitArgument;
                    groupParameters = new ArrayList<>();
                }
            }
            // We record the possible last existing group into the groupList
            recordExistingStatementGroup(groupSample, groupParameters, groupList, batchIndexTranslations); // Final group break
            // We correct newEntityIdIndexInBatch & newEntityIdIndexInGeneratedKeys
            for (Map.Entry<EntityId, Integer> entry : newEntityIdIndexInBatch.entrySet()) {
                EntityId newEntityId = entry.getKey();
                int batchIndex = entry.getValue();
                // Correcting index in batch
                int newBatchIndex = batchIndexTranslations.get(batchIndex);
                entry.setValue(newBatchIndex);
                // Correcting index in generated keys
                int newGeneratedKeyIndex = getGeneratedKeyIndexShift(batchIndex, batchIndexTranslations);
                newEntityIdIndexInGeneratedKeys.put(newEntityId, newGeneratedKeyIndex);
            }
            // We apply the resulting groupList into our submitArguments
            submitArguments.clear();
            submitArguments.addAll(groupList);
        }

        private boolean addToExistingStatementGroup(SubmitArgument submitArgument, SubmitArgument groupSample, List<Object[]> groupParameters) {
            String statement = submitArgument.getStatement();
            Object[] parameters = submitArgument.getParameters();
            if (parameters != null && groupSample != null && Objects.equals(statement, groupSample.getStatement())) {
                if (groupParameters.isEmpty()) {
                    groupParameters.add(groupSample.getParameters());
                }
                groupParameters.add(parameters);
                return true;
            }
            return false;
        }

        private void recordExistingStatementGroup(SubmitArgument groupSample, List<Object[]> groupParameters, List<SubmitArgument> groupList, List<Integer> batchIndexTranslations) {
            if (groupSample != null) {
                if (groupParameters == null || groupParameters.isEmpty()) { // means it was the only one in the group
                    groupList.add(groupSample); // so we just reinsert it as is
                    batchIndexTranslations.add(groupList.size() - 1);
                    translateParametersKeyReferences(groupSample.getParameters(), batchIndexTranslations, groupList);
                } else { // means there were several statements in the group
                    Batch<Object> parametersBatch = new Batch<>(groupParameters.toArray());
                    groupList.add(newSubmitArgument(groupSample.getLanguage(), groupSample.getStatement(), parametersBatch));
                    for (Object[] parameters : groupParameters) {
                        batchIndexTranslations.add(groupList.size() - 1);
                        translateParametersKeyReferences(parameters, batchIndexTranslations, groupList);
                    }
                }
            }
        }

        private void translateParametersKeyReferences(Object[] parameters, List<Integer> batchIndexTranslations, List<SubmitArgument> groupList) {
            for (int parameterIndex = 0, length = Arrays.length(parameters); parameterIndex < length; parameterIndex++) {
                Object value = parameters[parameterIndex];
                if (value instanceof GeneratedKeyReference) {
                    GeneratedKeyReference ref = (GeneratedKeyReference) value;
                    int batchIndex = ref.getStatementBatchIndex();
                    int generatedKeyIndex = ref.getGeneratedKeyIndex();
                    int newBatchIndex = batchIndexTranslations.get(batchIndex); // may be shorter than batchIndex if some earlier statements were grouped
                    int newGeneratedKeyIndex = generatedKeyIndex + getGeneratedKeyIndexShift(batchIndex, batchIndexTranslations);
                    if (newBatchIndex != batchIndex || newGeneratedKeyIndex != generatedKeyIndex)
                        parameters[parameterIndex] = new GeneratedKeyReference(newBatchIndex, newGeneratedKeyIndex);
                }
            }
        }

        private int getGeneratedKeyIndexShift(int batchIndex, List<Integer> batchIndexTranslations) {
            int newBatchIndex = batchIndexTranslations.get(batchIndex); // may be shorter than batchIndex if some earlier statements were grouped
            int shift = 0;
            for (int previousBatchIndex = batchIndex - 1; previousBatchIndex >= 0 && batchIndexTranslations.get(previousBatchIndex) == newBatchIndex; previousBatchIndex--) {
                shift++;
            }
            return shift;
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

        void addToBatch(String language, String statement, Object... parameters) {
            submitArguments.add(newSubmitArgument(language, statement, parameters));
        }

        SubmitArgument newSubmitArgument(String language, String statement, Object... parameters) {
            return SubmitArgument.builder()
                    .setDataSourceId(dataSourceId)
                    .setDataScope(dataScope)
                    .setLanguage(language)
                    .setStatement(statement)
                    .setParameters(parameters)
                    .build();
        }
    }
}
