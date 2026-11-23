package dev.webfx.stack.db.query.cli;

import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.uischeduler.AnimationFramePass;
import javafx.application.Application;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static dev.webfx.stack.db.query.cli.Checks.check;
import static dev.webfx.stack.db.query.cli.Checks.report;
import static dev.webfx.stack.db.query.cli.Checks.section;

/**
 * That the two stubs a terminal brings in place of a UI kit survive what a BOOT does to them.
 *
 * <h3>Why this check exists, and what it cost not to have it</h3>
 *
 * <p>{@code HeadlessKitLauncherProvider.launchApplication} threw {@code UnsupportedOperationException}, on the
 * reasoning that a command-line tool reaching it would be a bug. It is not a bug: it is the boot.
 * {@code WebFxKitLauncherModuleBooter} runs at {@code APPLICATION_LAUNCH_LEVEL} (8) in every WebFX executable
 * and calls it unconditionally, and {@code WebFxKitLauncher.launchApplication} wraps only the provider lookup
 * in a try/catch, not the call. Level 8 sits between the jobs initializer (7) and the jobs starter (9), so the
 * throw aborted the boot of every command that needs a server.
 *
 * <p>The fix went into the SPI: {@code WebFxKitLauncherProvider.launchApplication} now defaults to doing
 * nothing, so a provider with no application to launch inherits the right behaviour instead of rediscovering
 * it. This class therefore no longer overrides it, and the case below pins the PLATFORM default — restoring a
 * throw in {@code webfx-kit-launcher} fails this check, which is the point: the behaviour this tool depends on
 * lives in another repository now.
 *
 * <p>Nothing else would have found it. Every other check in this module is pure logic and never boots; the
 * failure needs a real boot, and the one that was run was read for a different question. So the cases below
 * are written as "what does a boot call on these objects, and what must happen".
 *
 * <h3>And the scheduler's half</h3>
 *
 * <p>{@code scheduleDeferred} must actually RUN the task. That is the whole reason the OpenJFX providers were
 * not reused: {@code OpenJFXUiSchedulerProvider} hands a deferred task to {@code WebFxKitLauncher.onReady}
 * while the kit is not ready, and a CLI never launches a JavaFX application, so the task waits for a moment
 * that does not come. A scheduler that silently stops running things is the failure shape this whole
 * instrument exists to avoid, so it is asserted rather than assumed.
 *
 * <p><b>Probed by deletion:</b> restoring a throw in the SPI's {@code launchApplication} default, and
 * separately making {@code isReady()} return true and {@code scheduleDeferred} a no-op, each made this check
 * fail; all were restored.
 *
 * @author Claude Code
 */
public class HeadlessProvidersCheck {

    public static void main(String[] args) throws InterruptedException {
        HeadlessKitLauncherProvider kit = new HeadlessKitLauncherProvider();

        section("What the boot calls on the kit launcher, at APPLICATION_LAUNCH_LEVEL");
        boolean launched = true;
        try {
            // Exactly the shape WebFxKitLauncherModuleBooter passes: a factory that yields null, because no
            // javafx Application service is registered in a terminal program.
            kit.launchApplication(() -> null);
        } catch (Throwable t) {
            launched = false;
        }
        check("launchApplication does NOT throw — a boot calls it unconditionally, and a throw aborts the boot",
              launched);
        boolean launchedWithArgs = true;
        try {
            kit.launchApplication(() -> null, "probe", "conversations-for-event");
        } catch (Throwable t) {
            launchedWithArgs = false;
        }
        check("nor with the main args the booter passes along", launchedWithArgs);
        boolean launchedWithApplication = true;
        try {
            // Should an Application service ever be registered, declining is still not throwing: the boot has
            // no catch for it, and a build mistake must not look like a crash in the middle of the CLI.
            kit.launchApplication(() -> (Application) null);
        } catch (Throwable t) {
            launchedWithApplication = false;
        }
        check("and not even when asked for an Application type", launchedWithApplication);
        check("the behaviour comes from the SPI default, not from an override here",
              java.util.Arrays.stream(HeadlessKitLauncherProvider.class.getDeclaredMethods())
                  .noneMatch(m -> "launchApplication".equals(m.getName())));

        section("The one answer the session code depends on");
        check("isReady() is false, which routes ClientSideStateSession's listener calls inline",
              !kit.isReady());
        StringBuilder ran = new StringBuilder();
        kit.onReady(() -> ran.append("ran"));
        check("onReady runs its runnable immediately rather than posting it to a thread that never drains",
              "ran".contentEquals(ran));
        check("safeAreaInsetsProperty answers a real property, since a caller may observe it",
              kit.safeAreaInsetsProperty() != null && kit.safeAreaInsetsProperty().get() != null);
        check("a stage is still refused — nothing in a CLI has any business asking for one",
              refuses(kit::getPrimaryStage));
        check("so is text measurement", refuses(() -> kit.measureBaselineOffset(null)));

        section("The scheduler runs what it is given — the reason the OpenJFX one was not reused");
        HeadlessUiSchedulerProvider scheduler = new HeadlessUiSchedulerProvider();
        CountDownLatch deferred = new CountDownLatch(1);
        scheduler.scheduleDeferred(deferred::countDown);
        check("scheduleDeferred actually runs the task", deferred.await(5, TimeUnit.SECONDS));
        CountDownLatch delayed = new CountDownLatch(1);
        scheduler.scheduleDelay(10, delayed::countDown);
        check("scheduleDelay actually runs the task", delayed.await(5, TimeUnit.SECONDS));
        CountDownLatch background = new CountDownLatch(1);
        scheduler.runInBackground(background::countDown);
        check("runInBackground actually runs the task", background.await(5, TimeUnit.SECONDS));
        CountDownLatch inFrame = new CountDownLatch(1);
        Scheduled frame = scheduler.scheduleDelayInAnimationFrame(0, inFrame::countDown, 0,
                                                                  AnimationFramePass.UI_UPDATE_PASS);
        check("an 'animation frame' task runs too — in a terminal that just means soon",
              inFrame.await(5, TimeUnit.SECONDS) && frame != null);
        check("isUiThread() is false, which is what makes runInUiThread defer instead of dropping",
              !scheduler.isUiThread());
        CountDownLatch uiThread = new CountDownLatch(1);
        dev.webfx.platform.scheduler.Scheduler.runInUiThread(uiThread::countDown);
        check("and Scheduler.runInUiThread runs it, because the constructor injected this provider "
              + "(unset, that method silently drops the runnable)",
              uiThread.await(5, TimeUnit.SECONDS));

        System.exit(report("HeadlessProvidersCheck"));
    }

    static boolean refuses(Runnable runnable) {
        try {
            runnable.run();
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }
}
