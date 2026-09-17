package dev.webfx.stack.orm.dql.querypush.interceptor;

import dev.webfx.stack.db.datascope.aggregate.AggregateScopeBuilder;
import dev.webfx.stack.orm.domainmodel.DomainField;
import dev.webfx.stack.orm.expression.Expression;
import dev.webfx.stack.orm.expression.terms.And;
import dev.webfx.stack.orm.expression.terms.Constant;
import dev.webfx.stack.orm.expression.terms.Equals;
import dev.webfx.stack.orm.expression.terms.ExpressionArray;
import dev.webfx.stack.orm.expression.terms.IdExpression;
import dev.webfx.stack.orm.expression.terms.ParameterReference;

import java.util.Set;

/**
 * Derives PARTITION entries (AggregateScope) from `field = value` terms of a
 * DQL statement, so the query-push pulse can skip queries whose partitions are
 * provably disjoint from a modification (ex: another event's subscriptions).
 *
 * Soundness rules:
 *  - Partition types are FIELD-qualified ("classId:fieldId"), never bare class
 *    names — `viewerPerson=X` and `assignee=Y` are different dimensions even
 *    though both reference Person, and conflating them under one "Person" type
 *    would produce false disjointness (missed refreshes).
 *  - Only terms under top-level ANDs are used; anything under OR/NOT is
 *    ignored (contributing nothing = conservative).
 *  - Only plain scalar values (numbers/strings) name a partition, normalized
 *    to String so boxing differences (Integer vs Long) can't fake
 *    disjointness. Unresolvable values (named parameters, generated-key
 *    references) are skipped — widening, never misleading.
 *
 * NOTE: mirrored in the submit interceptor module (the
 * WebFX-managed module graphs offer no common home for it) — keep both copies
 * in sync.
 *
 * @author Bruno Salmon
 */
final class DqlScopeUtil {

    private DqlScopeUtil() {}

    /**
     * Adds one partition entry per resolvable `field = value` term found in the
     * given expression (an AND-tree such as a where clause, or an
     * ExpressionArray such as a set clause).
     *
     * @param excludedFieldIds field ids contributing NO partition — used by
     *   updates to exclude SET fields from the where-derived partitions (a
     *   modified field's OLD value is unknown, so its equality is only valid
     *   for the after-state and would miss before-state watchers).
     * @param domainClassId the statement's domain class id — types the `id = value`
     *   partition of primary-key conditions (IdExpression carries no class itself)
     * @return the number of entries added (0 = nothing partitionable found)
     */
    static int addPartitions(AggregateScopeBuilder asb, Expression<?> terms, Object[] parameters, Set<Object> excludedFieldIds, Object domainClassId) {
        if (terms == null)
            return 0;
        if (terms instanceof And)
            return addPartitions(asb, ((And<?>) terms).getLeft(), parameters, excludedFieldIds, domainClassId)
                 + addPartitions(asb, ((And<?>) terms).getRight(), parameters, excludedFieldIds, domainClassId);
        if (terms instanceof ExpressionArray) {
            int count = 0;
            for (Expression<?> expression : ((ExpressionArray<?>) terms).getExpressions())
                count += addPartitions(asb, expression, parameters, excludedFieldIds, domainClassId);
            return count;
        }
        if (terms instanceof Equals) {
            Equals<?> equals = (Equals<?>) terms;
            Expression<?> left = equals.getLeft();
            if (left instanceof DomainField) {
                DomainField field = (DomainField) left;
                if (excludedFieldIds == null || !excludedFieldIds.contains(field.getId())) {
                    Object value = resolveScalarValue(equals.getRight(), parameters);
                    if (value != null) {
                        asb.addAggregate(partitionType(field), value);
                        return 1;
                    }
                }
            } else if (left instanceof IdExpression && domainClassId != null) {
                // Primary-key condition: partitions on the row's own identity —
                // matches the "<classId>:id" entries writes emit for FK parents.
                Object value = resolveScalarValue(equals.getRight(), parameters);
                if (value != null) {
                    asb.addAggregate(idPartitionType(domainClassId), value);
                    return 1;
                }
            }
        }
        return 0; // non-equality / non-AND terms contribute nothing (conservative)
    }

    /** Identity-partition type of a class ("classId:id"). */
    static String idPartitionType(Object domainClassId) {
        return domainClassId + ":id";
    }

    /** Field-qualified partition type — role-precise, see class javadoc. */
    private static String partitionType(DomainField field) {
        return field.getDomainClass().getId() + ":" + field.getId();
    }

    /** The term's scalar value normalized to String, or null when unusable. */
    static Object resolveScalarValue(Expression<?> right, Object[] parameters) {
        Object value = null;
        if (right instanceof Constant)
            value = ((Constant<?>) right).getConstantValue();
        else if (right instanceof ParameterReference) {
            int index = ((ParameterReference<?>) right).getIndex(); // $N syntax, 1-based; -1 for named
            if (parameters != null && index >= 1 && index <= parameters.length)
                value = parameters[index - 1];
        }
        return value instanceof Number || value instanceof String ? String.valueOf(value) : null;
    }
}
