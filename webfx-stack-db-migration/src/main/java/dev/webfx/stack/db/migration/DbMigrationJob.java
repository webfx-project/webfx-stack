package dev.webfx.stack.db.migration;

import dev.webfx.platform.boot.ApplicationReadiness;
import dev.webfx.platform.boot.spi.ApplicationJob;
import dev.webfx.platform.conf.ConfigLoader;
import dev.webfx.platform.console.Console;
import dev.webfx.platform.service.SingleServiceProvider;
import dev.webfx.stack.db.datasource.LocalDataSourceService;
import dev.webfx.stack.db.migration.spi.MigrationScriptsProvider;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Boot job that applies the pending DB migrations before the application becomes ready. onInit() runs at the
 * JOBS_INIT boot level, i.e. before the HTTP server starts, and registers a readiness gate that keeps /health
 * answering 503 until the migrations have been applied successfully. On failure everything has been rolled
 * back and the gate is never completed: the application stays alive but unhealthy, so a blue/green deployment
 * never switches traffic to it and rolls back to the previous version (which keeps running against the
 * untouched database). Failures are also recorded in the db_migration table (success = false rows).
 *
 * @author Bruno Salmon
 */
public final class DbMigrationJob implements ApplicationJob {

    private static final String READINESS_GATE_NAME = "db-migration";
    private static final String CONFIG_PATH = "webfx.stack.db.migration";
    // `webfx.stack.db.migration.apply` is deliberately THREE-STATE, and the difference between "false"
    // and "not set" is the whole point:
    //
    //   true     — apply them. The deployed image says this, via an override rendered beside the
    //              datasource one; nothing else should.
    //   false    — do not apply, and that is a decision someone took: complete the gate and carry on.
    //              This is what a developer machine sets, so the deployed pipeline is the only writer
    //              of a shared database's schema.
    //   NOT SET  — do not apply, and do NOT pretend to be ready. An instance that was never told
    //              whether it may write the schema is an instance nobody configured, and the failure we
    //              are guarding is a deployed build whose override went missing: it would otherwise come
    //              up perfectly healthy against a stale schema and fail later, scattered, at runtime.
    //              Refusing readiness keeps that loud, and stops a blue/green deployment switching to it.
    //
    // Defaulting to false rather than true is what makes a laptop harmless: migrations used to apply
    // unless you opted out, and the opt-out lived in a gitignored conf/ that nobody inherits — so a fresh
    // clone silently acquired the power to rewrite the schema of whatever database it was pointed at.
    private static final String APPLY_CONFIG_KEY = "apply";

    @Override
    public void onInit() {
        // Note: exceptions must never escape onInit() (the boot sequence doesn't handle them consistently);
        // every failure path below just logs and leaves the readiness gate pending.
        MigrationScriptsProvider scriptsProvider = SingleServiceProvider.getProvider(MigrationScriptsProvider.class,
            () -> ServiceLoader.load(MigrationScriptsProvider.class), SingleServiceProvider.NotFoundPolicy.RETURN_NULL);
        if (scriptsProvider == null) {
            log("No MigrationScriptsProvider registered — skipping DB migrations");
            return;
        }
        Runnable markReady = ApplicationReadiness.registerPendingReadinessGate(READINESS_GATE_NAME);
        List<MigrationScript> scripts;
        try {
            scripts = scriptsProvider.getMigrationScripts();
        } catch (RuntimeException e) {
            Console.error("❌ DB MIGRATION FAILED — the bundled migration scripts are invalid; this application will stay unhealthy (/health = 503)", e);
            return;
        }
        if (scripts.isEmpty()) {
            log("No DB migration scripts bundled");
            markReady.run();
            return;
        }
        ConfigLoader.onConfigLoaded(CONFIG_PATH, config -> { // config is null when the path is not declared anywhere
            Boolean configuredApply = config == null ? null : config.getBoolean(APPLY_CONFIG_KEY);
            if (Boolean.FALSE.equals(configuredApply)) {
                log("⏭️ DB migration apply is disabled on this instance (" + CONFIG_PATH + "." + APPLY_CONFIG_KEY + " = false) — " + scripts.size() + " scripts bundled but not applied; the deployed pipeline is responsible for applying them");
                markReady.run();
                return;
            }
            if (!Boolean.TRUE.equals(configuredApply)) {
                // Neither told to apply nor told not to. Staying unhealthy is the safe answer and the
                // legible one: on a deployed instance it means an override went missing and the build must
                // not take traffic; on a developer machine it means deciding, once, whether this instance
                // may write to the database it is pointed at — which is exactly the decision that used to
                // be made silently, in the affirmative, by doing nothing.
                Console.error("❌ DB MIGRATION NOT CONFIGURED — " + CONFIG_PATH + "." + APPLY_CONFIG_KEY
                    + " is not set, so the " + scripts.size() + " bundled scripts were NOT applied and this"
                    + " application will stay unhealthy (/health = 503) rather than run against a schema it"
                    + " may not match. Set it to true where migrations should be applied (the deployed"
                    + " image does this), or to false to say deliberately that this instance is not the"
                    + " writer of this database's schema.");
                return;
            }
            log("Waiting for the datasource before checking DB migrations (" + scripts.size() + " scripts bundled)...");
            LocalDataSourceService.onInitialised(() ->
                new DbMigrationRunner(scriptsProvider.getDataSourceId(), scripts).run()
                    .onSuccess(result -> {
                        log("✅ DB migration: " + result);
                        markReady.run();
                    })
                    .onFailure(e -> Console.error("❌ DB MIGRATION FAILED — all changes have been rolled back, and this application will stay unhealthy (/health = 503) so a blue/green deployment rolls back to the previous version. Check the db_migration table (success = false rows) and the logs above.", e)));
        });
    }
}
