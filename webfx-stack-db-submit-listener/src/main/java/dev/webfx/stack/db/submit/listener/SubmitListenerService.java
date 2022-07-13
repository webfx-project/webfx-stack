package dev.webfx.stack.db.submit.listener;

import dev.webfx.stack.db.submit.SubmitArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Salmon
 */
public final class SubmitListenerService {

    private static final List<SubmitListener> listeners = new ArrayList<>();

    public static void addSubmitListener(SubmitListener listener) {
        listeners.add(listener);
    }

    public static void removeSubmitListener(SubmitListener listener) {
        listeners.add(listener);
    }

    public static void fireSuccessfulSubmit(SubmitArgument... submitArgument) {
        listeners.forEach(l -> l.onSuccessfulSubmit(submitArgument));
    }
}
