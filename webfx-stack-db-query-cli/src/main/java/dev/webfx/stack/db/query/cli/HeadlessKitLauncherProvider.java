package dev.webfx.stack.db.query.cli;

import dev.webfx.kit.launcher.spi.WebFxKitLauncherProvider;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * The UI kit a terminal has, which is none — and whose only load-bearing answer is "not ready".
 *
 * <h3>Why a CLI needs one</h3>
 *
 * <p>{@code ClientSideStateSession.callRunnable} asks {@code WebFxKitLauncher.isReady()} before deciding
 * whether to post work to the UI thread, and {@code WebFxKitLauncher.getProvider()} THROWS when no provider
 * is registered. That call happens on the bus's first connection, so without this class the CLI dies at the
 * moment it connects.
 *
 * <h3>The one answer that matters</h3>
 *
 * <p>{@link #isReady()} is false. That is not a placeholder: it routes the session's listener calls through
 * the inline branch, whose comment explains that running them immediately is correct while only the
 * application logic has started — which, for a CLI, is permanently. Returning true would post every listener
 * call to a UI thread that does not exist.
 *
 * <p>Everything else here is unreachable by the CLI's own code path: nothing it does measures text, opens a
 * stage, reads a clipboard or asks for screen insets. Those methods therefore refuse rather than invent an
 * answer — a CLI that somehow reaches {@code getPrimaryStage()} has a bug worth seeing, and a zero-sized
 * {@code Bounds} returned from {@code measureText} would hide it. The one exception is
 * {@link #safeAreaInsetsProperty()}, which returns a real empty property because a property is a value a
 * caller may legitimately observe without ever reading a layout.
 *
 * <h3>Launching: nothing to override</h3>
 *
 * <p>{@code launchApplication} is absent on purpose. A boot calls it unconditionally —
 * {@code WebFxKitLauncherModuleBooter} runs at {@code APPLICATION_LAUNCH_LEVEL}, between the jobs initializer
 * and the jobs starter — so for a terminal program "there is nothing to launch" is the normal path, and the
 * SPI's default does nothing. This class used to override it with a refusal, which aborted the boot of every
 * command that needs a server; the fix went into the SPI so the next headless provider inherits it. See
 * {@code WebFxKitLauncherProvider.launchApplication}.
 *
 * <h3>Why not the OpenJFX provider</h3>
 *
 * <p>Same reason as {@link HeadlessUiSchedulerProvider}: taking the OpenJFX kit would make deferred tasks wait
 * for a JavaFX application that a CLI never launches. The JavaFX classes in these signatures are compile-time
 * only — this class never touches the toolkit, so no JavaFX runtime is initialised.
 *
 * @author Claude Code
 */
public final class HeadlessKitLauncherProvider implements WebFxKitLauncherProvider {

    private final ReadOnlyObjectProperty<Insets> safeAreaInsets = new SimpleObjectProperty<>(Insets.EMPTY);

    /**
     * False, and this is the whole point of the class. See the class comment.
     */
    @Override
    public boolean isReady() {
        return false;
    }

    /**
     * Runs immediately. The default implementation posts to the UI thread, which in a CLI means "never" —
     * and the callers of {@code onReady} are precisely the ones whose work must not be dropped.
     */
    @Override
    public void onReady(Runnable runnable) {
        runnable.run();
    }

    @Override
    public HostServices getHostServices() {
        throw headless("host services");
    }

    @Override
    public boolean isStageProgrammaticallyRelocatableAndResizable() {
        return false;
    }

    @Override
    public Stage getPrimaryStage() {
        throw headless("a primary stage");
    }

    @Override
    public Application getApplication() {
        throw headless("a JavaFX application");
    }

    @Override
    public Bounds measureText(String text, Font font) {
        throw headless("text measurement");
    }

    @Override
    public double measureBaselineOffset(Font font) {
        throw headless("text measurement");
    }

    @Override
    public ReadOnlyObjectProperty<Insets> safeAreaInsetsProperty() {
        return safeAreaInsets;
    }

    private static UnsupportedOperationException headless(String what) {
        return new UnsupportedOperationException(
            "This process has no UI kit, so it cannot provide " + what
            + ". Reaching this from a command-line tool is a bug, not a missing feature.");
    }
}
