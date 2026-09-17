package dev.webfx.stack.db.migration.spi;

import dev.webfx.stack.db.migration.MigrationScript;

import java.util.List;

/**
 * SPI implemented by the application module that bundles the migration scripts in its resources (typically
 * loaded with {@link dev.webfx.stack.db.migration.MigrationScriptReader#readFromIndex(Class, String)}). When
 * no provider is registered, the migration job is dormant.
 *
 * @author Bruno Salmon
 */
public interface MigrationScriptsProvider {

    /**
     * The id of the (local) datasource the migrations must be applied to.
     */
    Object getDataSourceId();

    /**
     * The migration scripts bundled in the application, in ascending version order. Implementations may throw
     * a RuntimeException on a malformed bundle, which fails the migration (the application never becomes ready).
     */
    List<MigrationScript> getMigrationScripts();

}
