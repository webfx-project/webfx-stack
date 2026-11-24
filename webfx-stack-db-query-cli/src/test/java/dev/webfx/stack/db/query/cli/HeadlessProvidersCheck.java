package dev.webfx.stack.db.query.cli;

import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.uischeduler.AnimationFramePass;
import dev.webfx.platform.uischeduler.spi.UiSchedulerProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static dev.webfx.stack.db.query.cli.Checks.check;
import static dev.webfx.stack.db.query.cli.Checks.report;
import static dev.webfx.stack.db.query.cli.Checks.section;

/**
 * That the scheduler a terminal brings in place of a UI kit survives what a BOOT does to it, and answers the
 * one question the session code asks.
 *
 * <h3>The kit launcher is gone, and that is the point</h3>
 *
 * <p>This class used to assert the behaviour of a {@code HeadlessKitLauncherProvider}, because a CLI had to
 * supply one: {@code WebFxKitLauncherModuleBooter} runs at {@code APPLICATION_LAUNCH_LEVEL} (8) in every WebFX
 * executable and calls {@code launchApplication} unconditionally — it threw, and the boot of every command that
 * needs a server died with it. That was fixed in the SPI (the method now defaults to doing nothing), and the
 * provider then existed only to be found.
 *
 * <p>It no longer exists at all. {@code ClientSideStateSession} asked {@code WebFxKitLauncher.isReady()} to
 * decide between running a listener inline and marshalling it to a UI thread, which made
 * {@code webfx-stack-session-state-client} — and so EVERY client that holds a session — require
 * {@code webfx-kit-launcher} and, through the kit's javafx emulation modules, real JavaFX. A terminal program
 * carried javafx-base and javafx-graphics, and booted a UI kit launcher, to answer one boolean that is always
 * false.
 *
 * <p>That question moved to {@code UiSchedulerProvider.isUiThreadReady()}, where it belongs: it is about the
 * scheduler. So a headless client needs no kit, and this module's classpath now holds neither
 * {@code webfx-kit-*} nor javafx. The cases below pin what is left, which is the part this tool depends on.
 *
 * <h3>The scheduler's half, which has not changed</h3>
 *
 * <p>{@code scheduleDeferred} must actually RUN the task. That is the whole reason the OpenJFX providers were
 * not reused: {@code OpenJFXUiSchedulerProvider} hands a deferred task to {@code WebFxKitLauncher.onReady}
 * while the kit is not ready, and a CLI never launches a JavaFX application, so the task waits for a moment
 * that does not come. A scheduler that silently stops running things is the failure shape this whole
 * instrument exists to avoid, so it is asserted rather than assumed.
 *
 * <p>Nothing else would have found the original defect. Every other check in this module is pure logic and
 * never boots.
 *
 * @author Claude Code
 */
public class HeadlessProvidersCheck {

    public static void main(String[] args) throws InterruptedException {
        section("The one answer the session code depends on, now asked of the scheduler");
        HeadlessUiSchedulerProvider headless = new HeadlessUiSchedulerProvider();
        check("isUiThreadReady() is false, which routes ClientSideStateSession's listener calls inline",
              !headless.isUiThreadReady());
        // The INTERFACE's default, reached through a proxy that implements nothing but it — so this asserts
        // the platform's choice rather than any provider's, and fails if that default is ever flipped to true.
        UiSchedulerProvider spiDefault = (UiSchedulerProvider) Proxy.newProxyInstance(
            UiSchedulerProvider.class.getClassLoader(), new Class<?>[] {UiSchedulerProvider.class},
            (proxy, method, arguments) ->
                method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, arguments) : null);
        check("and the SPI's own default is false too, so a provider that forgets to answer still gets the "
              + "safe one — marshalling to a UI thread that does not exist is the failure that hangs",
              !spiDefault.isUiThreadReady());

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
